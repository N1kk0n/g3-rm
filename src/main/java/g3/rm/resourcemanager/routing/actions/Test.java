package g3.rm.resourcemanager.routing.actions;

import g3.rm.resourcemanager.routing.dtos.kafka.Content;

public class Test implements Action {
    @Override
    public int execute(Content content) {
        return 2;
    }
}
