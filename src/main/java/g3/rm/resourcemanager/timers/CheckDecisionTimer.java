package g3.rm.resourcemanager.timers;

import g3.rm.resourcemanager.services.DecisionUpdateService;
import g3.rm.resourcemanager.services.TimerCreatorService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.TimerTask;

public class CheckDecisionTimer extends TimerTask {
    @Autowired
    private DecisionUpdateService decisionUpdateService;

    @Override
    public void run() {
        decisionUpdateService.updateDecision();
    }
}
