package g3.rm.resourcemanager.services;

import g3.rm.resourcemanager.entities.ManagerParam;
import g3.rm.resourcemanager.repositories.ManagerParamRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class LogCleanerService {
    @Autowired
    private ManagerParamRepository managerParamRepository;
    @Autowired
    private TimerCreatorService timerCreatorService;

    private final Logger LOGGER = LogManager.getLogger("LogCleanerService");

    public void cleanOldTaskLogs() {
        ManagerParam maxAgeParam = managerParamRepository.getByParamName("LOG_MAX_AGE");
        if (maxAgeParam == null) {
            LOGGER.error("Manager parameter LOG_MAX_AGE not found");
            return;
        }
        int maxAge = Integer.parseInt(maxAgeParam.getParamValue());

        ManagerParam logPathParam = managerParamRepository.getByParamName("TASK_LOG_DIR");
        if (logPathParam == null) {
            LOGGER.error("Manager parameter TASK_LOG_DIR not found");
            return;
        }
        String logPath = logPathParam.getParamValue();

        File logDir = new File(logPath);
        if (!logDir.exists()) {
            LOGGER.error("Directory: " + logDir.getAbsolutePath() + " not found");
            return;
        }
        if (!logDir.isDirectory()) {
            LOGGER.error("File: " + logDir.getAbsolutePath() + " is not directory");
            return;
        }

        boolean debugMode = debugMode();
        for (File taskLogDir : logDir.listFiles()) {
            deleteLog(taskLogDir, maxAge, debugMode);
        }

        timerCreatorService.createLogCleanerTimer();
    }

    private void deleteLog(File log, long maxAge, boolean debugMode) {
        try {
            FileTime lastModifiedTime = Files.getLastModifiedTime(log.toPath());
            Instant lastModifiedInstant = lastModifiedTime.toInstant();
            Instant nowInstant = Clock.systemUTC().instant();
            Duration difference = Duration.between(lastModifiedInstant, nowInstant);
            long days = difference.toDays();
            if (days <= maxAge) {
                return;
            }
            if (log.isDirectory()) {
                for (File file : log.listFiles()) {
                    deleteLog(file, maxAge, debugMode);
                }
            }
            if (debugMode) {
                LOGGER.info("Deleting [DEBUG MODE] (no actions): " + log.getAbsolutePath());
            } else {
                log.delete();
            }
        } catch (IOException ex) {
            LOGGER.error("File " + log.getAbsolutePath() + " not found", ex);
            return;
        }
    }

    private boolean debugMode() {
        ManagerParam debugModeParam = managerParamRepository.getByParamName("AGENT_DEBUG_MODE");
        if (debugModeParam == null) {
            LOGGER.error("Manager parameter with name: AGENT_DEBUG_MODE not found");
            return false;
        }
        String debugMode = debugModeParam.getParamValue();
        debugMode = debugMode.toLowerCase();
        if (debugMode.equals("1") || debugMode.equals("true")) {
            return true;
        }
        return false;
    }
}
