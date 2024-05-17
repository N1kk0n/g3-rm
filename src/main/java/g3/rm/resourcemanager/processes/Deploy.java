package g3.rm.resourcemanager.processes;

import g3.rm.resourcemanager.entities.ManagerParam;
import g3.rm.resourcemanager.entities.ProgramParam;
import g3.rm.resourcemanager.repositories.ManagerParamRepository;
import g3.rm.resourcemanager.repositories.ProgramParamRepository;
import g3.rm.resourcemanager.services.SessionEventResponseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import g3.rm.resourcemanager.dtos.Task;
import g3.rm.resourcemanager.router.RouterEventPublisher;
import g3.rm.resourcemanager.services.FileSystemService;
import g3.rm.resourcemanager.services.LoggerService;
import g3.rm.resourcemanager.services.TimerCreatorService;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;

public class Deploy {
    @Autowired
    private ManagerParamRepository managerParamRepository;
    @Autowired
    private ProgramParamRepository programParamRepository;
    @Autowired
    private FileSystemService fileSystemService;
    @Autowired
    private SessionEventResponseService responseService;
    @Autowired
    private LoggerService loggerService;
    @Autowired
    private TimerCreatorService timerCreatorService;
    @Autowired
    private RouterEventPublisher eventPublisher;

    private Task task;

    private final Logger LOGGER = LogManager.getLogger("Deploy");
    private final String OPERATION = "DEPLOY";
    private final String SUCCESS = "DEPLOY_DONE";
    private final String ERROR = "DEPLOY_ERROR";

    public Deploy() {
    }

    public Task getTaskObject() {
        return task;
    }

    public void setTaskObject(Task task) {
        this.task = task;
    }

    @Async
    public CompletableFuture<String> start() {
        long taskId = this.task.getTaskId();
        Timer timer = timerCreatorService.createDeployTimer(taskId);

        responseService.setDeployInitEvent(task.getTaskId(), task.getSessionId());
        ProgramParam programParam = programParamRepository.findByProgramIdAndParamName(this.task.getProgramId(), OPERATION);
        if (programParam == null) {
            LOGGER.error("Unknown programId: " + task.getProgramId());
            timerCreatorService.cancelTimer(timer);
            responseService.setDeployDoneEvent(this.task.getTaskId(), task.getSessionId(),-1);
            eventPublisher.publishTaskEvent(ERROR, task);
            return CompletableFuture.completedFuture(ERROR);
        }
        String path = programParam.getParamValue();
        if (fileSystemService.templateMarkerExists(this.task.getProgramId())) {
            String programHome = programParamRepository.findByProgramIdAndParamName(this.task.getProgramId(), "HOME").getParamValue();
            String scriptName = managerParamRepository.getByParamName("DEPLOY_NAME").getParamValue();
            path = programHome + File.separator + this.task.getTaskId() + "_scripts" + File.separator + scriptName;
        }
        if(!new File(path).exists()) {
            LOGGER.error("File: " + path + " not found");
            timerCreatorService.cancelTimer(timer);
            responseService.setDeployDoneEvent(this.task.getTaskId(), task.getSessionId(),-1);
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
        int exitCode;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(outputLogPath + File.separator + OPERATION.toLowerCase() + ".log")));
            processBuilder.redirectError(ProcessBuilder.Redirect.appendTo(new File(errorLogPath + File.separator + OPERATION.toLowerCase() + "_error.log")));
            process = processBuilder.start();

            LOGGER.info("Operation: " + OPERATION + " (Task ID: " + task.getTaskId() + "). Start process: " + args);

            process.waitFor();
            exitCode = process.exitValue();

            LOGGER.info("Operation: " + OPERATION + " (Task ID: " + task.getTaskId() + "). Exit value: " + exitCode);

            loggerService.saveLog(this.task.getTaskId(), this.task.getSessionId(), OPERATION);
        } catch (IOException ex) {
            LOGGER.error("Process execution error. Operation: " + OPERATION + ". Start process: " + args + ". Message: " + ex.getMessage(), ex);
            timerCreatorService.cancelTimer(timer);
            responseService.setDeployDoneEvent(this.task.getTaskId(), task.getSessionId(),-1);
            eventPublisher.publishTaskEvent(ERROR, task);
            return CompletableFuture.completedFuture(ERROR);
        } catch (InterruptedException ex) {
            LOGGER.error("Process was interrupted. Operation: " + OPERATION + ". Start process: " + args);
            responseService.setDeployDoneEvent(this.task.getTaskId(), task.getSessionId(),-1);
            eventPublisher.publishTaskEvent(ERROR, task);
            return CompletableFuture.completedFuture(ERROR);
        }
        responseService.setDeployDoneEvent(this.task.getTaskId(), task.getSessionId(), exitCode);
        if (exitCode != 0) {
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent(ERROR, task);
            return CompletableFuture.completedFuture(ERROR);
        }
        timerCreatorService.cancelTimer(timer);
        eventPublisher.publishTaskEvent(SUCCESS, task);
        return CompletableFuture.completedFuture(SUCCESS);
    }
}
