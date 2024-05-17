package g3.rm.resourcemanager.processes;

import g3.rm.resourcemanager.entities.ProgramParam;
import g3.rm.resourcemanager.repositories.ManagerParamRepository;
import g3.rm.resourcemanager.repositories.ProgramParamRepository;
import g3.rm.resourcemanager.services.SessionEventResponseService;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import g3.rm.resourcemanager.dtos.Task;
import g3.rm.resourcemanager.router.RouterEventPublisher;
import g3.rm.resourcemanager.services.FileSystemService;
import g3.rm.resourcemanager.services.TimerCreatorService;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;

public class ProgressInfo {
    @Autowired
    private ManagerParamRepository managerParamRepository;
    @Autowired
    private ProgramParamRepository programParamRepository;
    @Autowired
    private FileSystemService fileSystemService;
    @Autowired
    private SessionEventResponseService responseService;
    @Autowired
    private TimerCreatorService timerCreatorService;
    @Autowired
    private RouterEventPublisher eventPublisher;

    private Task task;

    private final Logger LOGGER = LogManager.getLogger("ProgressInfo");
    private final String OPERATION = "PROGRESSINFO";
    private final String SUCCESS = "PROGRESSINFO_DONE";
    private final String ERROR = "PROGRESSINFO_ERROR";

    public ProgressInfo() {
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
        Timer timer = timerCreatorService.createProgressInfoTimer(taskId);

        ProgramParam programParam = programParamRepository.findByProgramIdAndParamName(this.task.getProgramId(), OPERATION);
        if (programParam == null) {
            LOGGER.error("Unknown programId: " + this.task.getProgramId());
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent(ERROR, task);
            return CompletableFuture.completedFuture(ERROR);
        }
        String path = programParam.getParamValue();
        if (fileSystemService.templateMarkerExists(this.task.getProgramId())) {
            String programHome = programParamRepository.findByProgramIdAndParamName(this.task.getProgramId(), "HOME").getParamValue();
            String scriptName = managerParamRepository.getByParamName("PROGRESS_INFO_NAME").getParamValue();
            path = programHome + File.separator + this.task.getTaskId() + "_scripts" + File.separator + scriptName;
        }
        if (!new File(path).exists()) {
            LOGGER.error("File: " + path + " not found");
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent(ERROR, task);
            return CompletableFuture.completedFuture(ERROR);
        }

        List<String> args = new LinkedList<>();
        args.add(path);
        args.add(String.valueOf(this.task.getTaskId()));

        Process process;
        int exitCode;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            process = processBuilder.start();

            LOGGER.info("Operation: " + OPERATION + " (Task ID: " + task.getTaskId() + "). Start process: " + args);

            process.waitFor();
            exitCode = process.exitValue();

            JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] pair = line.split(":");
                if (pair.length != 2) {
                    continue;
                }
                String key = pair[0].trim();
                String value = pair[1].trim();
                if (key.isEmpty() || value.isEmpty()) {
                    continue;
                }
                //check <value> is digit
                try {
                    Long.parseLong(value);
                } catch (NumberFormatException e) {
                    continue;
                }
                jsonObjectBuilder.add(key, value);
            }

            jsonObjectBuilder.add("taskId", this.task.getTaskId());
            jsonObjectBuilder.add("sessionId", this.task.getSessionId());
            String progressInfo = jsonObjectBuilder.build().toString();

            LOGGER.info("Operation: " + OPERATION + " (Task ID: " + task.getTaskId() + "). Exit value: " + exitCode);
            responseService.setProgressInfo(progressInfo);
        } catch (IOException ex) {
            LOGGER.error("Process execution error. Operation: " + OPERATION + ". Start process: " + args + ". Message: " + ex.getMessage(), ex);
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent(ERROR, task);
            return CompletableFuture.completedFuture(ERROR);
        } catch (InterruptedException ex) {
            LOGGER.error("Process was interrupted. Operation: " + OPERATION + ". Start process: " + args);
            eventPublisher.publishTaskEvent(ERROR, task);
            return CompletableFuture.completedFuture(ERROR);
        }
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
