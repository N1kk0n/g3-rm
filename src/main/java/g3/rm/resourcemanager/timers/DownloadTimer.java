package g3.rm.resourcemanager.timers;

import g3.rm.resourcemanager.entities.TaskProcess;
import g3.rm.resourcemanager.repositories.TaskProcessRepository;
import g3.rm.resourcemanager.services.ProcessContainerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Iterator;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

public class DownloadTimer extends TimerTask {
    @Autowired
    private TaskProcessRepository processRepository;
    @Autowired
    private ProcessContainerService containerService;

    private long taskId;

    private final Logger LOGGER = LogManager.getLogger("DownloadTimer");

    public long getTaskId() {
        return this.taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    @Override
    public void run() {
        LOGGER.info("Interrupting download procedure. Task ID: " + taskId);

        Iterable<TaskProcess> processes = processRepository.findAllByEntityIdAndOperation(taskId, "DOWNLOAD");
        Iterator<TaskProcess> taskProcessIterator = processes.iterator();
        while(taskProcessIterator.hasNext()) {
            TaskProcess process = taskProcessIterator.next();

            long stageId = process.getStageId();
            CompletableFuture<String> future = containerService.getProcess(stageId);
            future.cancel(true);
        }
    }
}
