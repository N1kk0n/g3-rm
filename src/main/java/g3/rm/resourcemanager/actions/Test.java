package g3.rm.resourcemanager.actions;

import g3.rm.resourcemanager.dtos.kafka.Content;

public class Test implements Action {
    @Override
    public int execute(Content content) {
        return 2;
    }
}
