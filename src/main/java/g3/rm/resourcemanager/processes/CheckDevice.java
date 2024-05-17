package g3.rm.resourcemanager.processes;

import g3.rm.resourcemanager.dtos.Task;
import g3.rm.resourcemanager.entities.DeviceParam;
import g3.rm.resourcemanager.repositories.DeviceParamRepository;
import g3.rm.resourcemanager.router.RouterEventPublisher;
import g3.rm.resourcemanager.services.SessionEventResponseService;
import g3.rm.resourcemanager.services.TimerCreatorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;

public class CheckDevice {
    @Autowired
    private DeviceParamRepository deviceParamRepository;
    @Autowired
    private SessionEventResponseService responseService;
    @Autowired
    private TimerCreatorService timerCreatorService;
    @Autowired
    private RouterEventPublisher eventPublisher;

    private Task task;

    private final Logger LOGGER = LogManager.getLogger("CheckDevice");
    private final String OPERATION = "CHECKDEVICE";
    private final String SUCCESS = "CHECKDEVICE_DONE";
    private final String ERROR = "CHECKDEVICE_ERROR";

    public CheckDevice() {
    }

    public Task getTaskObject() {
        return task;
    }

    public void setTaskObject(Task task) {
        this.task = task;
    }

    @Async
    public CompletableFuture<String> start() {
        String deviceName = task.getDeviceNameList().get(0);

        Timer timer = timerCreatorService.createCheckDeviceTimer(deviceName);

        DeviceParam deviceParam = deviceParamRepository.findByDeviceNameAndParamName(deviceName, "CHECK_PATH");
        if (deviceParam == null) {
            LOGGER.error("Unknown device name: " + deviceName + " with paramName: CHECK_PATH");
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent(ERROR, task);
            return CompletableFuture.completedFuture(ERROR);
        }
        String checkScriptPath = deviceParam.getParamValue();
        if(!new File(checkScriptPath).exists()) {
            LOGGER.error("File: " + checkScriptPath + " not found");
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent(ERROR, task);
            return CompletableFuture.completedFuture(ERROR);
        }

        List<String> args = new LinkedList<>();
        args.add(checkScriptPath);
        
        Process process;
        int exitCode;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            process = processBuilder.start();

            LOGGER.info("Operation: " + OPERATION + " (Device name: " + deviceName + "). Start process: " + args);

            process.waitFor();
            exitCode = process.exitValue();

            LOGGER.info("Operation: " + OPERATION + " (Device name: " + deviceName + "). Exit value: " + exitCode);
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
        timerCreatorService.cancelTimer(timer);
        if (exitCode != 0) {
            eventPublisher.publishTaskEvent(ERROR, task);
            return CompletableFuture.completedFuture(ERROR);
        }
        eventPublisher.publishTaskEvent(SUCCESS, task);
        return CompletableFuture.completedFuture(SUCCESS);
    }
}
