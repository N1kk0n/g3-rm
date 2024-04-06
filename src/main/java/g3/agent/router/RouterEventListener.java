package g3.agent.router;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import g3.agent.data.TaskObject;
import g3.agent.jpa_domain.TaskProcess;
import g3.agent.repositories.TaskProcessRepository;
import g3.agent.services.FileSystemService;
import g3.agent.services.HttpResponseService;
import g3.agent.services.ProcessContainerService;
import g3.agent.services.ProcessCreatorService;

import java.util.Iterator;

@Component
public class RouterEventListener implements ApplicationListener<RouterEvent> {
    @Autowired
    private TaskProcessRepository taskProcessRepository;
    @Autowired
    private ProcessCreatorService processCreatorService;
    @Autowired
    private ProcessContainerService processContainerService;
    @Autowired
    private HttpResponseService httpResponseService;
    @Autowired
    private FileSystemService fileSystemService;

    private final Logger LOGGER = LogManager.getLogger("RouterEventListener");

    @Override
    public void onApplicationEvent(RouterEvent routerEvent) {
        String taskMessage = routerEvent.getMessage();
        TaskObject taskObject = routerEvent.getTaskObject();
        String operation = taskMessage.split("_")[0];

        Iterable<TaskProcess> processes = taskProcessRepository.findAllByEntityIdAndOperation(taskObject.getTaskId(), operation);
        Iterator<TaskProcess> taskProcessIterator = processes.iterator();
        while(taskProcessIterator.hasNext()) {
            TaskProcess taskProcess = taskProcessIterator.next();
            processContainerService.removeProcess(taskProcess.getStageId());
        }

        switch (taskMessage) {
            case "DOWNLOAD_DONE": {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): DOWNLOAD_DONE. Next operation: DEPLOY");
                processCreatorService.create("DEPLOY", taskObject);
                break;
            }
                
            case "DOWNLOAD_ERROR": {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): DOWNLOAD_ERROR. Clean work folder.");
                taskObject.setSessionStatus("ERROR");
                endSession(taskObject, true);
                break;
            }
                
            case "DEPLOY_DONE": {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): DEPLOY_DONE. Next operation: RUN");
                processCreatorService.create("RUN", taskObject);
                break;
            }
                
            case "DEPLOY_ERROR": {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): DEPLOY_ERROR. Clean work folder.");
                taskObject.setSessionStatus("ERROR");
                endSession(taskObject, true);
                break;
            }
                
            case "RUN_DONE": {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): RUN_DONE. Next operation: COLLECT");
                taskObject.setSessionStatus("DONE");
                processCreatorService.create("FINALPROGRESSINFO", taskObject);
                processCreatorService.create("COLLECT", taskObject);
                break;
            }
                
            case "RUN_STOP": {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): RUN_STOP. Next operation: COLLECT");
                taskObject.setSessionStatus("STOP");
                processCreatorService.create("COLLECT", taskObject);
                break;
            }
                
            case "RUN_ERROR": {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): RUN_ERROR. Next operation: COLLECT");
                taskObject.setSessionStatus("ERROR");
                processCreatorService.create("COLLECT", taskObject);
                break;
            }
                
            case "COLLECT_DONE": {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): COLLECT_DONE. Next operation: UPLOAD");
                processCreatorService.create("UPLOAD", taskObject);
                break;
            }
                
            case "COLLECT_ERROR": {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): COLLECT_ERROR. Save RUN results.");
                taskObject.setSessionStatus("ERROR");
                endSession(taskObject, false);
                break;
            }
                
            case "UPLOAD_DONE": {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): UPLOAD_DONE. Clean work folder.");
                endSession(taskObject, true);
                break;
            }
                
            case "UPLOAD_ERROR": {
                LOGGER.info(taskObject.getTaskId() + " (session: " + taskObject.getSessionId() + "): UPLOAD_ERROR. Save RUN results.");
                taskObject.setSessionStatus("ERROR");
                endSession(taskObject, false);
                break;
            }
        }
    }

    private void endSession(TaskObject taskObject, boolean clearWorkFolder) {
        switch (taskObject.getSessionStatus()) {
            case "DONE": 
                httpResponseService.sendSessionEnd(taskObject.getEventId(),
                                                   taskObject.getTaskId(),
                                                   taskObject.getSessionId(), 0);
                break;
            case "STOP": 
                httpResponseService.sendSessionStop(taskObject.getTaskId(),
                                                    taskObject.getSessionId(), 0);
                break;
            case "ERROR": 
                httpResponseService.sendSessionEnd(taskObject.getEventId(),
                                                   taskObject.getTaskId(),
                                                   taskObject.getSessionId(), -1);
                break;
        }
        if (clearWorkFolder) {
            fileSystemService.removeTaskFolder(taskObject.getTaskId(), taskObject.getProgramId());
        }
        fileSystemService.removeScriptsFolder(taskObject.getTaskId(), taskObject.getProgramId());
    }
}

