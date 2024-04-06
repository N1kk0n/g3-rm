package g3.agent.router;

import org.springframework.context.ApplicationEvent;
import g3.agent.data.TaskObject;

public class RouterEvent extends ApplicationEvent {
    private String message;
    private TaskObject taskObject;

    public RouterEvent(Object source) {
        super(source);
    }

    public RouterEvent(Object source, String message, TaskObject taskObject) {
        super(source);
        this.message = message;
        this.taskObject = taskObject;
    }

    public String getMessage() {
        return message;
    }

    public TaskObject getTaskObject() {
        return taskObject;
    }
}
