package g3.rm.resourcemanager.services;

import g3.rm.resourcemanager.jpa_domain.TaskProcess;
import g3.rm.resourcemanager.repositories.TaskProcessRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class ProcessContainerService {
    @Autowired
    private TaskProcessRepository taskProcessRepository;

    private Map<Long, CompletableFuture<String>> processMap = new HashMap<>();

    private final Logger LOGGER = LogManager.getLogger("ProcessContainerService");

    public void clearTaskProcesses() {
        taskProcessRepository.deleteAll();
    }

    public void addProcess(CompletableFuture<String> process, String operation, long entityId) {
        long stageId = generateStageId();
        TaskProcess taskProcess = new TaskProcess();
        taskProcess.setStageId(stageId);
        taskProcess.setOperation(operation);
        taskProcess.setEntityId(entityId);
        taskProcessRepository.save(taskProcess);
        processMap.put(stageId, process);
        LOGGER.info("Add task process [stageId: " + taskProcess.getStageId() +
                                    " operation: " + taskProcess.getOperation() + 
                                     " entityId: " + taskProcess.getEntityId() + "]");
    }

    public boolean checkExist(String operation, long entityId) {
        Iterable<TaskProcess> processes = taskProcessRepository.findAllByEntityIdAndOperation(entityId, operation);
        Iterator<TaskProcess> taskProcessIterator = processes.iterator();
        if(taskProcessIterator.hasNext()) {
            return true;
        } else {
            return false;
        }
    }

    public CompletableFuture<String> getProcess(long stageId) {
        if (processMap.containsKey(stageId)) {
            return processMap.get(stageId);
        }
        return null;
    }

    public void removeProcess(long stageId) {
        TaskProcess process = taskProcessRepository.findByStageId(stageId);
        if (process != null) {
            taskProcessRepository.delete(process);
        }
        processMap.remove(stageId);
        LOGGER.info("Remove task process [stageId: " + process.getStageId() +
                                       " operation: " + process.getOperation() + 
                                        " entityId: " + process.getEntityId() + "]");
    }

    private long generateStageId() {
        long stageId;
        long min = 1000L;
        long max = 10000L;
        do {
            stageId = (long)(Math.random() * (max - min + 1) + min);
        } while (processMap.containsKey(stageId));
        return stageId;
    }
}
