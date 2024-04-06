package g3.agent.timers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import g3.agent.jpa_domain.TaskProcess;
import g3.agent.repositories.TaskProcessRepository;
import g3.agent.services.ProcessContainerService;

import java.util.Iterator;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

public class UploadTimer extends TimerTask {
    @Autowired
    private TaskProcessRepository processRepository;
    @Autowired
    private ProcessContainerService containerService;

    private long taskId;

    private final Logger LOGGER = LogManager.getLogger("UploadTimer");

    public long getTaskId() {
        return this.taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    @Override
    public void run() {
        LOGGER.info("Interrupting upload procedure. Task ID: " + taskId);

        Iterable<TaskProcess> processes = processRepository.findAllByEntityIdAndOperation(taskId, "UPLOAD");
        Iterator<TaskProcess> taskProcessIterator = processes.iterator();
        while(taskProcessIterator.hasNext()) {
            TaskProcess process = taskProcessIterator.next();

            long stageId = process.getStageId();
            CompletableFuture<String> future = containerService.getProcess(stageId);
            future.cancel(true);
        }
    }
}
