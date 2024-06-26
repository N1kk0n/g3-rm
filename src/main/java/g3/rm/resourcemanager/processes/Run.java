package g3.rm.resourcemanager.processes;

import g3.rm.resourcemanager.entities.ProgramParam;
import g3.rm.resourcemanager.repositories.ManagerParamRepository;
import g3.rm.resourcemanager.repositories.ProgramParamRepository;
import g3.rm.resourcemanager.services.SessionEventResponseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import g3.rm.resourcemanager.dtos.Task;
import g3.rm.resourcemanager.entities.ManagerParam;
import g3.rm.resourcemanager.router.RouterEventPublisher;
import g3.rm.resourcemanager.services.FileSystemService;
import g3.rm.resourcemanager.services.LoggerService;
import g3.rm.resourcemanager.services.ProcessContainerService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Run {
    @Autowired
    private ManagerParamRepository managerParamRepository;
    @Autowired
    private ProgramParamRepository programParamRepository;
    @Autowired
    private SessionEventResponseService responseService;
    @Autowired
    private LoggerService loggerService;
    @Autowired
    private FileSystemService fileSystemService;
    @Autowired
    private ProcessContainerService processContainerService;
    @Autowired
    private RouterEventPublisher eventPublisher;
    

    private Task task;

    private final Logger LOGGER = LogManager.getLogger("Run");
    private final String OPERATION = "RUN";
    private final String SUCCESS = "RUN_DONE";
    private final String ERROR = "RUN_ERROR";
    private final String STOP = "RUN_STOP";

    public Run() {
    }

    public Task getTaskObject() {
        return task;
    }

    public void setTaskObject(Task task) {
        this.task = task;
    }

    @Async
    public CompletableFuture<String> start() {
        responseService.sendRunInitEvent(task.getTaskId(), task.getSessionId());
        ProgramParam programParam = programParamRepository.findByProgramIdAndParamName(this.task.getProgramId(), OPERATION);
        if (programParam == null) {
            LOGGER.error("Unknown programId: " + this.task.getProgramId());
            responseService.sendRunDoneEvent(task.getTaskId(),
                                                task.getSessionId(),
                                                task.getDeviceNameList(), -1, -1, "");
            eventPublisher.publishTaskEvent(ERROR, task);
            return CompletableFuture.completedFuture(ERROR);
        }
        String path = programParam.getParamValue();
        if (fileSystemService.templateMarkerExists(this.task.getProgramId())) {
            String programHome = programParamRepository.findByProgramIdAndParamName(this.task.getProgramId(), "HOME").getParamValue();
            String scriptName = managerParamRepository.getByParamName("RUN_NAME").getParamValue();
            path = programHome + File.separator + this.task.getTaskId() + "_scripts" + File.separator + scriptName;
        }
        if (!new File(path).exists()) {
            LOGGER.error("File: " + path + " not found");
            responseService.sendRunDoneEvent(task.getTaskId(),
                                                task.getSessionId(),
                                                task.getDeviceNameList(), -1, -1, "");
            eventPublisher.publishTaskEvent(ERROR, task);
            return CompletableFuture.completedFuture(ERROR);
        }

        ManagerParam managerParam = managerParamRepository.getByParamName("TASK_LOG_DIR");
        String logDir = managerParam.getParamValue();
        String outputLogPath = logDir + File.separator + task.getTaskId() + File.separator + task.getSessionId();
        String errorLogPath = logDir + File.separator + task.getTaskId() + File.separator + task.getSessionId();
        File outputLog = new File(outputLogPath);
        if (!outputLog.exists()) {
            outputLog.mkdirs();
        }
        File errorLog = new File(errorLogPath);
        if (!errorLog.exists()) {
            errorLog.mkdirs();
        }

        List<String> args = new LinkedList<>();
        args.add(path);
        args.add(String.valueOf(this.task.getTaskId()));

        Process process;
        int exitCode = -1;
        int programCode = 1;
        String boomerangCode = "";
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(outputLogPath + File.separator + OPERATION.toLowerCase() + ".log")));
            processBuilder.redirectError(ProcessBuilder.Redirect.appendTo(new File(errorLogPath + File.separator + OPERATION.toLowerCase() + "_error.log")));
            process = processBuilder.start();

            LOGGER.info("Operation: " + OPERATION + " (Task ID: " + task.getTaskId() + "). Start process: " + args);

            process.waitFor();
            exitCode = process.exitValue();

            boomerangCode = getBoomerangCode();
            programCode = getProgramCode();

            LOGGER.info("Operation: " + OPERATION + " (Task ID: " + task.getTaskId() + "). Exit value: " + exitCode);

            loggerService.saveLog(this.task.getTaskId(), this.task.getSessionId(), OPERATION);
            
            if (fileSystemService.stopFlagExists(this.task.getTaskId(), this.task.getSessionId(), this.task.getProgramId()))
                throw new InterruptedException("Process was interrupted. Operation: " + OPERATION + ". Start process: " + args);
        
        } catch (IOException ex) {
            LOGGER.error("Process execution error. Operation: " + OPERATION + ". Start process: " + args + ". Message: " + ex.getMessage(), ex);
            responseService.sendRunDoneEvent(task.getTaskId(),
                                                task.getSessionId(),
                                                task.getDeviceNameList(), exitCode, programCode, boomerangCode);
            eventPublisher.publishTaskEvent(ERROR, task);
            return CompletableFuture.completedFuture(ERROR);
        } catch (InterruptedException ex) {
            LOGGER.info(ex.getMessage());
            waitForStopCompletion(this.task.getTaskId());
            
            responseService.sendRunDoneEvent(task.getTaskId(),
                                                task.getSessionId(),
                                                task.getDeviceNameList(), exitCode, programCode, boomerangCode);
            eventPublisher.publishTaskEvent(STOP, task);
            return CompletableFuture.completedFuture(STOP);
        }
        responseService.sendRunDoneEvent(task.getTaskId(),
                                            task.getSessionId(),
                                            task.getDeviceNameList(), exitCode, programCode, boomerangCode);
        if (exitCode != 0) {
            eventPublisher.publishTaskEvent(ERROR, task);
            return CompletableFuture.completedFuture(ERROR);
        }
        eventPublisher.publishTaskEvent(SUCCESS, task);
        return CompletableFuture.completedFuture(SUCCESS);
    }

    private int getProgramCode() {
        int programCode = 1;
        try {
            ProgramParam programParam = programParamRepository.findByProgramIdAndParamName(this.task.getProgramId(), "HOME");
            String homeDir = programParam.getParamValue();
            String programCodePath = homeDir + File.separator + this.task.getTaskId() + "/" + this.task.getTaskId() + ".kod";
            File taskCodeFile = new File(programCodePath);
            if (!taskCodeFile.exists()) {
                return programCode;
            }
            FileReader fileReader = new FileReader(taskCodeFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String programCodeStr = bufferedReader.readLine();
            bufferedReader.close();
            fileReader.close();
            if (programCodeStr.contains("=")) {
                programCodeStr = programCodeStr.split("=")[1];
            }
            programCode = Integer.parseInt(programCodeStr);
        } catch (IOException ex) {
            LOGGER.error("Error while get program code: " + ex.getMessage(), ex);
        }
        return programCode;
    }

    private String getBoomerangCode() {
        String boomerangCode = "";
        try {
            ProgramParam programParam = programParamRepository.findByProgramIdAndParamName(this.task.getProgramId(), "HOME");
            String homeDir = programParam.getParamValue();
            String taskCodePath = homeDir + File.separator + this.task.getTaskId() + "/TaskCode";
            File taskCodeFile = new File(taskCodePath);
            if (!taskCodeFile.exists()) {
                return boomerangCode;
            }
            FileReader fileReader = new FileReader(taskCodeFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            boomerangCode = bufferedReader.readLine();
            bufferedReader.close();
            fileReader.close();
        } catch (IOException ex) {
            LOGGER.error("Error while get boomerang code: " + ex.getMessage(), ex);
        }
        return boomerangCode;
    }

    private void waitForStopCompletion(long taskId) {
        try {
            LOGGER.info("Waiting for STOP complete...");
            while (true) {
                if (processContainerService.checkExist("STOP", taskId)) {
                    Thread.sleep(10000);
                } else {
                    return;
                }    
            }
        } catch (Exception e) {
            return;
        }
    }
}
