package g3.rm.resourcemanager.timers;

import g3.rm.resourcemanager.services.TimerCreatorService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.TimerTask;

public class CheckDecisionTimer extends TimerTask {
    @Autowired
    private TimerCreatorService timerCreatorService;

    @Override
    public void run() {
        try {
            System.out.println("works");
        } finally {
            timerCreatorService.createCheckDecisionTimer();
        }
    }
}
