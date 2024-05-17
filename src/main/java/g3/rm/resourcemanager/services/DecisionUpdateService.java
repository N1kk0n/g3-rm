package g3.rm.resourcemanager.services;

import g3.rm.resourcemanager.dtos.DecisionItem;
import g3.rm.resourcemanager.dtos.Task;
import g3.rm.resourcemanager.repositories.DecisionRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

@Service
public class DecisionUpdateService {
    @Autowired
    private DecisionRepository decisionRepository;
    @Autowired
    private TimerCreatorService timerCreatorService;

    private final Logger LOGGER = LogManager.getLogger("DecisionUpdateService");

    public void updateDecision() {
        try {
            List<DecisionItem> decisionItems = decisionRepository.getDecision();
            if (decisionItems.isEmpty()) {
                timerCreatorService.createCheckDecisionTimer();
            } else {
                LOGGER.info("New decision found: " + decisionItems);

                List<Task> newTaskList = getTasksList(decisionItems);
                for (Task task : newTaskList) {
                    timerCreatorService.createStartTaskCountdown(task);
                }
            }
        } catch (Exception e) {
            timerCreatorService.createCheckDecisionTimer();
        }
    }

    private List<Task> getTasksList(List<DecisionItem> decisionItems) {
        List<Task> taskList = new LinkedList<>();

        Task task;
        List<String> deviceNameList = null;
        HashSet<Long> taskIds = new HashSet<>();

        for (DecisionItem item : decisionItems) {
            long taskId = item.getTaskId();
            String deviceName = item.getDeviceName();
            if (!taskIds.contains(taskId)) {
                taskIds.add(taskId);

                task = new Task();
                task.setTaskId(taskId);

                deviceNameList = new LinkedList<>();
                deviceNameList.add(deviceName);

                task.setDeviceNameList(deviceNameList);
                taskList.add(task);
            } else {
                deviceNameList.add(deviceName);
            }
        }
        return taskList;
    }
}
