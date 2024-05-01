package g3.rm.timers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import g3.rm.jpa_domain.LogicalDeviceParam;
import g3.rm.jpa_domain.TaskProcess;
import g3.rm.repositories.DeviceParamRepository;
import g3.rm.repositories.TaskProcessRepository;
import g3.rm.services.ProcessContainerService;

import java.util.Iterator;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

public class CheckDeviceTimer extends TimerTask {
    @Autowired
    private DeviceParamRepository deviceParamRepository;
    @Autowired
    private TaskProcessRepository processRepository;
    @Autowired
    private ProcessContainerService containerService;

    private String deviceName;

    private final Logger LOGGER = LogManager.getLogger("CheckDeviceTimer");

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    @Override
    public void run() {
        LOGGER.info("Interrupting check device procedure. Device name: " + deviceName);

        LogicalDeviceParam deviceParam = deviceParamRepository.findByDeviceNameAndParamName(deviceName, "CHECK_PATH");
        if (deviceParam == null) {
            LOGGER.error("Device with name: " + deviceName + " not found in LOGICAL_DEVICE_PARAM");
        }
        long deviceId = deviceParam.getDeviceId();
        Iterable<TaskProcess> processes = processRepository.findAllByEntityIdAndOperation(deviceId, "CHECKDEVICE");
        Iterator<TaskProcess> taskProcessIterator = processes.iterator();
        while(taskProcessIterator.hasNext()) {
            TaskProcess process = taskProcessIterator.next();

            long stageId = process.getStageId();
            CompletableFuture<String> future = containerService.getProcess(stageId);
            future.cancel(true);
        }
    }
}
