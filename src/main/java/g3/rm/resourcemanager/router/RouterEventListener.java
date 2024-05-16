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
import g3.rm.resourcemanager.dtos.TaskObject;

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
        TaskObject taskObject = routerEvent.getTaskObject();
        String operation = taskMessage.split("_")[0];

        Iterable<TaskProcess> processes = taskProcessRepository.findAllByEntityIdAndOperation(taskObject.getTaskId(), operation);
        for (TaskProcess taskProcess : processes) {
            processContainerService.removeProcess(taskProcess.getStageId());
        }

        switch (taskMessage) {
            case "DOWNLOAD_DONE" -> {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): DOWNLOAD_DONE. Next operation: DEPLOY");
                processCreatorService.create("DEPLOY", taskObject);
            }
            case "DOWNLOAD_ERROR" -> {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): DOWNLOAD_ERROR. Clean work folder.");
                taskObject.setSessionStatus("ERROR");
                endSession(taskObject, true);
            }
            case "DEPLOY_DONE" -> {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): DEPLOY_DONE. Next operation: RUN");
                processCreatorService.create("RUN", taskObject);
            }
            case "DEPLOY_ERROR" -> {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): DEPLOY_ERROR. Clean work folder.");
                taskObject.setSessionStatus("ERROR");
                endSession(taskObject, true);
            }
            case "RUN_DONE" -> {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): RUN_DONE. Next operation: COLLECT");
                taskObject.setSessionStatus("DONE");
                processCreatorService.create("FINALPROGRESSINFO", taskObject);
                processCreatorService.create("COLLECT", taskObject);
            }
            case "RUN_STOP" -> {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): RUN_STOP. Next operation: COLLECT");
                taskObject.setSessionStatus("STOP");
                processCreatorService.create("COLLECT", taskObject);
            }
            case "RUN_ERROR" -> {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): RUN_ERROR. Next operation: COLLECT");
                taskObject.setSessionStatus("ERROR");
                processCreatorService.create("COLLECT", taskObject);
            }
            case "COLLECT_DONE" -> {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): COLLECT_DONE. Next operation: UPLOAD");
                processCreatorService.create("UPLOAD", taskObject);
            }
            case "COLLECT_ERROR" -> {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): COLLECT_ERROR. Save RUN results.");
                taskObject.setSessionStatus("ERROR");
                endSession(taskObject, false);
            }
            case "UPLOAD_DONE" -> {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): UPLOAD_DONE. Clean work folder.");
                endSession(taskObject, true);
            }
            case "UPLOAD_ERROR" -> {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): UPLOAD_ERROR. Save RUN results.");
                taskObject.setSessionStatus("ERROR");
                endSession(taskObject, false);
            }
        }
    }

    private void endSession(TaskObject taskObject, boolean clearWorkFolder) {
        switch (taskObject.getSessionStatus()) {
            case "DONE": 
                responseService.sendSessionEnd(taskObject.getTaskId(),
                                                   taskObject.getSessionId(), 0);
                break;
            case "STOP": 
                responseService.sendSessionStop(taskObject.getTaskId(),
                                                    taskObject.getSessionId(), 0);
                break;
            case "ERROR": 
                responseService.sendSessionEnd(taskObject.getTaskId(),
                                                   taskObject.getSessionId(), -1);
                break;
        }
        if (clearWorkFolder) {
            fileSystemService.removeTaskFolder(taskObject.getTaskId(), taskObject.getProgramId());
        }
        fileSystemService.removeScriptsFolder(taskObject.getTaskId(), taskObject.getProgramId());
    }
}

