package g3.rm.resourcemanager.processes;

import g3.rm.resourcemanager.data.TaskObject;
import g3.rm.resourcemanager.jpa_domain.LogicalDeviceParam;
import g3.rm.resourcemanager.repositories.DeviceParamRepository;
import g3.rm.resourcemanager.services.HttpResponseService;
import g3.rm.resourcemanager.router.RouterEventPublisher;
import g3.rm.resourcemanager.services.TimerService;
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
    private HttpResponseService responseService;
    @Autowired
    private TimerService timerService;
    @Autowired
    private RouterEventPublisher eventPublisher;

    private TaskObject taskObject;

    private final Logger LOGGER = LogManager.getLogger("CheckDevice");
    private final String OPERATION = "CHECKDEVICE";
    private final String SUCCESS = "CHECKDEVICE_DONE";
    private final String ERROR = "CHECKDEVICE_ERROR";

    public CheckDevice() {
    }

    public TaskObject getTaskObject() {
        return taskObject;
    }

    public void setTaskObject(TaskObject taskObject) {
        this.taskObject = taskObject;
    }

    @Async
    public CompletableFuture<String> start() {
        long eventId = taskObject.getEventId();
        String deviceName = taskObject.getDeviceNameList().get(0);

        Timer timer = timerService.createCheckDeviceTimer(deviceName);

        LogicalDeviceParam logicalDeviceParam = deviceParamRepository.findByDeviceNameAndParamName(deviceName, "CHECK_PATH");
        if (logicalDeviceParam == null) {
            LOGGER.error("Unknown device name: " + deviceName + " with paramName: CHECK_PATH");
            responseService.sendCheckDeviceResponse(eventId, deviceName, -1);
            timerService.cancelTimer(timer);
            eventPublisher.publishTaskEvent(ERROR, taskObject);
            return CompletableFuture.completedFuture(ERROR);
        }
        String checkScriptPath = logicalDeviceParam.getParamValue();
        if(!new File(checkScriptPath).exists()) {
            LOGGER.error("File: " + checkScriptPath + " not found");
            responseService.sendCheckDeviceResponse(eventId, deviceName, -1);
            timerService.cancelTimer(timer);
            eventPublisher.publishTaskEvent(ERROR, taskObject);
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
            responseService.sendCheckDeviceResponse(eventId, deviceName, -1);
            timerService.cancelTimer(timer);
            eventPublisher.publishTaskEvent(ERROR, taskObject);
            return CompletableFuture.completedFuture(ERROR);
        } catch (InterruptedException ex) {
            LOGGER.error("Process was interrupted. Operation: " + OPERATION + ". Start process: " + args);
            responseService.sendCheckDeviceResponse(eventId, deviceName, -1);
            eventPublisher.publishTaskEvent(ERROR, taskObject);
            return CompletableFuture.completedFuture(ERROR);
        }
        responseService.sendCheckDeviceResponse(eventId, deviceName, exitCode);
        timerService.cancelTimer(timer);
        if (exitCode != 0) {
            eventPublisher.publishTaskEvent(ERROR, taskObject);
            return CompletableFuture.completedFuture(ERROR);
        }
        eventPublisher.publishTaskEvent(SUCCESS, taskObject);
        return CompletableFuture.completedFuture(SUCCESS);
    }
}
