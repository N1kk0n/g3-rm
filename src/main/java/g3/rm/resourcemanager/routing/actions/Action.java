package g3.rm.resourcemanager.routing.actions;

import g3.rm.resourcemanager.routing.dtos.kafka.Content;

public interface Action {
    int execute(Content content);
}
