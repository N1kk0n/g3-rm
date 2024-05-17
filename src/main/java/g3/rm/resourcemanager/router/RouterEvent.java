package g3.rm.resourcemanager.router;

import org.springframework.context.ApplicationEvent;
import g3.rm.resourcemanager.dtos.Task;

public class RouterEvent extends ApplicationEvent {
    private String message;
    private Task task;

    public RouterEvent(Object source) {
        super(source);
    }

    public RouterEvent(Object source, String message, Task task) {
        super(source);
        this.message = message;
        this.task = task;
    }

    public String getMessage() {
        return message;
    }

    public Task getTaskObject() {
        return task;
    }
}
