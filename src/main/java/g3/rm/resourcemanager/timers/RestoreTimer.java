package g3.rm.resourcemanager.timers;

import g3.rm.resourcemanager.jpa_domain.RestoreStep;
import g3.rm.resourcemanager.repositories.RestoreStepRepository;
import g3.rm.resourcemanager.services.HttpResponseService;
import g3.rm.resourcemanager.services.TimerService;
import jakarta.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.TimerTask;

public class RestoreTimer extends TimerTask {
    @Autowired
    private RestoreStepRepository restoreStepRepository;
    @Autowired
    private TimerService timerService;
    @Autowired
    private HttpResponseService httpResponseService;

    private final Logger LOGGER = LogManager.getLogger("RestoreTimer");

    @Override
    public void run() {
        List<RestoreStep> restoreSteps = restoreStepRepository.findByOrderById();
        if (restoreSteps == null || restoreSteps.size() == 0) {
            LOGGER.info("Nothing to restore");
            return;
        }
        try {
            for (RestoreStep step : restoreSteps) {
                LOGGER.info("Restore call: " + step.getUrl());
                JsonObject response = httpResponseService.sendRestRequest(step.getUrl(), HttpMethod.GET.name(), "");
                if (requestCorrect(response)) {
                    restoreStepRepository.delete(step);
                } else {
                    timerService.createRestoreTimer();
                    return;
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException ex) {
            LOGGER.error("Restore process was interrupted: " + ex.getMessage(), ex);
        }
    }

    private boolean requestCorrect(JsonObject response) {
        if (response.containsKey("code") && response.getInt("code") == 200) {
            return true;
        } else {
            return false;
        }
    }
}
