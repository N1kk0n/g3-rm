package g3.rm.resourcemanager.actions;

import g3.rm.resourcemanager.dtos.kafka.Content;

public interface Action {
    int execute(Content content);
}
