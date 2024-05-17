package g3.rm.resourcemanager.router;

import g3.rm.resourcemanager.entities.TaskProcess;
import g3.rm.resourcemanager.repositories.TaskProcessRepository;
import g3.rm.resourcemanager.services.FileSystemService;
import g3.rm.resourcemanager.services.ProcessContainerService;
import g3.rm.resourcemanager.services.ProcessCreatorService;
import g3.rm.resourcemanager.services.SessionEventResponseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import g3.rm.resourcemanager.dtos.Task;

@Component
public class RouterEventListener implements ApplicationListener<RouterEvent> {
    @Autowired
    private TaskProcessRepository taskProcessRepository;
    @Autowired
    private ProcessCreatorService processCreatorService;
    @Autowired
    private ProcessContainerService processContainerService;
    @Autowired
    private SessionEventResponseService responseService;
    @Autowired
    private FileSystemService fileSystemService;

    private final Logger LOGGER = LogManager.getLogger("RouterEventListener");

    @Override
    public void onApplicationEvent(RouterEvent routerEvent) {
        String taskMessage = routerEvent.getMessage();
        Task task = routerEvent.getTaskObject();
        String operation = taskMessage.split("_")[0];

        Iterable<TaskProcess> processes = taskProcessRepository.findAllByEntityIdAndOperation(task.getTaskId(), operation);
        for (TaskProcess taskProcess : processes) {
            processContainerService.removeProcess(taskProcess.getStageId());
        }

        switch (taskMessage) {
            case "DOWNLOAD_DONE" -> {
                LOGGER.info(task.getTaskId() + " (session: " + task.getSessionId() + "): DOWNLOAD_DONE. Next operation: DEPLOY");
                processCreatorService.create("DEPLOY", task);
            }
            case "DOWNLOAD_ERROR" -> {
                LOGGER.info(task.getTaskId() + " (session: " + task.getSessionId() + "): DOWNLOAD_ERROR. Clean work folder.");
                task.setSessionStatus("ERROR");
                endSession(task, true);
            }
            case "DEPLOY_DONE" -> {
                LOGGER.info(task.getTaskId() + " (session: " + task.getSessionId() + "): DEPLOY_DONE. Next operation: RUN");
                processCreatorService.create("RUN", task);
            }
            case "DEPLOY_ERROR" -> {
                LOGGER.info(task.getTaskId() + " (session: " + task.getSessionId() + "): DEPLOY_ERROR. Clean work folder.");
                task.setSessionStatus("ERROR");
                endSession(task, true);
            }
            case "RUN_DONE" -> {
                LOGGER.info(task.getTaskId() + " (session: " + task.getSessionId() + "): RUN_DONE. Next operation: COLLECT");
                task.setSessionStatus("DONE");
                processCreatorService.create("FINALPROGRESSINFO", task);
                processCreatorService.create("COLLECT", task);
            }
            case "RUN_STOP" -> {
                LOGGER.info(task.getTaskId() + " (session: " + task.getSessionId() + "): RUN_STOP. Next operation: COLLECT");
                task.setSessionStatus("STOP");
                processCreatorService.create("COLLECT", task);
            }
            case "RUN_ERROR" -> {
                LOGGER.info(task.getTaskId() + " (session: " + task.getSessionId() + "): RUN_ERROR. Next operation: COLLECT");
                task.setSessionStatus("ERROR");
                processCreatorService.create("COLLECT", task);
            }
            case "COLLECT_DONE" -> {
                LOGGER.info(task.getTaskId() + " (session: " + task.getSessionId() + "): COLLECT_DONE. Next operation: UPLOAD");
                processCreatorService.create("UPLOAD", task);
            }
            case "COLLECT_ERROR" -> {
                LOGGER.info(task.getTaskId() + " (session: " + task.getSessionId() + "): COLLECT_ERROR. Save RUN results.");
                task.setSessionStatus("ERROR");
                endSession(task, false);
            }
            case "UPLOAD_DONE" -> {
                LOGGER.info(task.getTaskId() + " (session: " + task.getSessionId() + "): UPLOAD_DONE. Clean work folder.");
                endSession(task, true);
            }
            case "UPLOAD_ERROR" -> {
                LOGGER.info(task.getTaskId() + " (session: " + task.getSessionId() + "): UPLOAD_ERROR. Save RUN results.");
                task.setSessionStatus("ERROR");
                endSession(task, false);
            }
        }
    }

    private void endSession(Task task, boolean clearWorkFolder) {
        switch (task.getSessionStatus()) {
            case "DONE"  -> responseService.sendSessionEnd(task.getTaskId(), task.getSessionId(), 0);
            case "STOP"  -> responseService.sendSessionStop(task.getTaskId(), task.getSessionId(), 0);
            case "ERROR" -> responseService.sendSessionEnd(task.getTaskId(), task.getSessionId(), -1);
        }
        if (clearWorkFolder) {
            fileSystemService.removeTaskFolder(task.getTaskId(), task.getProgramId());
        }
        fileSystemService.removeScriptsFolder(task.getTaskId(), task.getProgramId());
    }
}

