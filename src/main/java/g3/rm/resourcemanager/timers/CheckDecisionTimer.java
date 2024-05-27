package g3.rm.resourcemanager.timers;

import g3.rm.resourcemanager.services.DecisionUpdateService;
import g3.rm.resourcemanager.utils.SingletonTimerWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.TimerTask;

public class CheckDecisionTimer extends TimerTask {
    @Autowired
    private DecisionUpdateService decisionUpdateService;
    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void run() {
        SingletonTimerWrapper singletonTimerWrapper = applicationContext.getBean(SingletonTimerWrapper.class);
        singletonTimerWrapper.purgeTimer("DecisionCreatorTimer");

        decisionUpdateService.updateDecision();
    }
}
