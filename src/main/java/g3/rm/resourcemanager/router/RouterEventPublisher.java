package g3.rm.resourcemanager.router;

import g3.rm.resourcemanager.services.ProcessContainerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import g3.rm.resourcemanager.dtos.TaskObject;

@Component
public class RouterEventPublisher {
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private ProcessContainerService processContainerService;

    private final Logger LOGGER = LogManager.getLogger("RouterEventPublisher");

    public void publishTaskEvent(String message, TaskObject taskObject) {
        String operation = message.substring(0, message.lastIndexOf("_"));
        waitForInsertCompletion(operation, taskObject.getTaskId());
        
        RouterEvent routerEvent = new RouterEvent(this, message, taskObject);
        eventPublisher.publishEvent(routerEvent);
    }

    private void waitForInsertCompletion(String operation, long entityId) {
        try {
            LOGGER.info("Waiting for insert [ " + operation + ", "  +  entityId + " ] complete...");
            while (true) {
                if (processContainerService.checkExist(operation, entityId)) {
                    return;
                }
                Thread.sleep(2000);    
            }
        } catch (Exception e) {
            return;
        }
    }
}
