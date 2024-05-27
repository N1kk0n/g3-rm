package g3.rm.resourcemanager.utils;

import java.util.HashMap;
import java.util.Timer;

public class SingletonTimerWrapper {
    private final HashMap<String, Timer> singletonContainer = new HashMap<>();

    public void storeTimer(String timerName, Timer timer) {
        this.singletonContainer.put(timerName, timer);
    }

    public boolean existsTimer(String timerName) {
        return singletonContainer.containsKey(timerName);
    }

    public void purgeTimer(String timerName) {
        Timer timer = this.singletonContainer.remove(timerName);
        timer.purge();
    }
}
