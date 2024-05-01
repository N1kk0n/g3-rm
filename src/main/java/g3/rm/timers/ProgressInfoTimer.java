package g3.rm.timers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import g3.rm.jpa_domain.TaskProcess;
import g3.rm.repositories.TaskProcessRepository;
import g3.rm.services.ProcessContainerService;

import java.util.Iterator;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

public class ProgressInfoTimer extends TimerTask {
    @Autowired
    private TaskProcessRepository processRepository;
    @Autowired
    private ProcessContainerService containerService;

    private long taskId;

    private final Logger LOGGER = LogManager.getLogger("ProgressInfoTimer");

    public long getTaskId() {
        return this.taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    @Override
    public void run() {
        LOGGER.info("Interrupting progress info procedure. Task ID: " + taskId);

        Iterable<TaskProcess> processes = processRepository.findAllByEntityIdAndOperation(taskId, "PROGRESSINFO");
        Iterator<TaskProcess> taskProcessIterator = processes.iterator();
        while(taskProcessIterator.hasNext()) {
            TaskProcess process = taskProcessIterator.next();

            long stageId = process.getStageId();
            CompletableFuture<String> future = containerService.getProcess(stageId);
            future.cancel(true);
        }
    }
}
