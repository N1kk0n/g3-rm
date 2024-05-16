package g3.rm.resourcemanager.timers;

import g3.rm.resourcemanager.entities.ManagerParam;
import g3.rm.resourcemanager.repositories.ManagerParamRepository;
import g3.rm.resourcemanager.services.LogCleanerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;

public class LogCleanerTimer extends TimerTask {
    @Autowired
    private ManagerParamRepository managerParamRepository;
    @Autowired
    private LogCleanerService logCleanerService;

    private final Logger LOGGER = LogManager.getLogger("LogCleanerTimer");

    @Override
    public void run() {
        ManagerParam maxAgeParam = managerParamRepository.getByParamName("LOG_MAX_AGE");
        if (maxAgeParam == null) {
            LOGGER.error("Agent parameter LOG_MAX_AGE not found");
            return;
        }
        int maxAge = Integer.parseInt(maxAgeParam.getParamValue());
        int daysMinus = maxAge * -1;

        Date now = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_YEAR, daysMinus);
        LOGGER.info("Clean logs with modification date older than: " + simpleDateFormat.format(calendar.getTime()));

        logCleanerService.cleanOldTaskLogs();
    }
}
