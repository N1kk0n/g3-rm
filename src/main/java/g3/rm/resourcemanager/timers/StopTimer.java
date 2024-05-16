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

public class StopTimer extends TimerTask {
    @Autowired
    private TaskProcessRepository processRepository;
    @Autowired
    private ProcessContainerService containerService;

    private long taskId;

    private final Logger LOGGER = LogManager.getLogger("StopTimer");

    public long getTaskId() {
        return this.taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    @Override
    public void run() {
       LOGGER.info("Interrupting stop task procedure. Task ID: " + taskId);

        Iterable<TaskProcess> stopProcesses = processRepository.findAllByEntityIdAndOperation(taskId, "STOP");
        Iterator<TaskProcess> stopProcessIterator = stopProcesses.iterator();
        while(stopProcessIterator.hasNext()) {
            TaskProcess process = stopProcessIterator.next();

            long stageId = process.getStageId();
            CompletableFuture<String> future = containerService.getProcess(stageId);
            future.cancel(true);
        }

        Iterable<TaskProcess> runProcesses = processRepository.findAllByEntityIdAndOperation(taskId, "RUN");
        Iterator<TaskProcess> runProcessIterator = runProcesses.iterator();
        while(runProcessIterator.hasNext()) {
            TaskProcess process = runProcessIterator.next();

            long stageId = process.getStageId();
            CompletableFuture<String> future = containerService.getProcess(stageId);
            future.cancel(true);
        }
    }
}
