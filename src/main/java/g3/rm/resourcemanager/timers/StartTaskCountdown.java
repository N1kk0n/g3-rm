package g3.rm.resourcemanager.timers;

import g3.rm.resourcemanager.dtos.Task;
import g3.rm.resourcemanager.repositories.SessionRepository;
import g3.rm.resourcemanager.services.ProcessCreatorService;
import g3.rm.resourcemanager.services.TimerCreatorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.TimerTask;

public class StartTaskCountdown extends TimerTask {
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private TimerCreatorService timerCreatorService;
    @Autowired
    private ProcessCreatorService processCreatorService;

    private final Logger LOGGER = LogManager.getLogger("StartTaskCountdown");

    private Task task;

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    @Override
    public void run() {
        try {
            if (sessionRepository.anotherSessionExists(task.getTaskId())) {
                LOGGER.info("Restart countdown for task ID: " + task.getTaskId() + ", program ID: " + task.getProgramId() + ", device list: " + task.getDeviceNameList());

                timerCreatorService.createStartTaskCountdown(task);
            } else {
                processCreatorService.create("DOWNLOAD", task);
            }
        } catch (Exception e) {
            timerCreatorService.createStartTaskCountdown(task);
        }
    }
}
