package g3.rm.resourcemanager.services;

import g3.rm.resourcemanager.entities.ManagerParam;
import g3.rm.resourcemanager.repositories.ManagerParamRepository;
import g3.rm.resourcemanager.timers.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Timer;

@Service
public class TimerCreatorService {
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ManagerParamRepository managerParamRepository;

    private final Logger LOGGER = LogManager.getLogger("TimerService");

    public Timer createCheckDeviceTimer(String deviceName) {
        if (debugMode()) {
            LOGGER.debug("Timer [DEBUG MODE]: CheckDeviceTimer not created");
            return null;
        }
        CheckDeviceTimer deviceTimer = applicationContext.getBean(CheckDeviceTimer.class);
        deviceTimer.setDeviceName(deviceName);

        ManagerParam managerParam = managerParamRepository.getByParamName("CHECKDEVICE_TIMEOUT");
        if (managerParam == null) {
            LOGGER.error("Agent param with name: CHECKDEVICE_TIMEOUT not found");
            return null;
        }
        long timerDelay = Long.parseLong(managerParam.getParamValue());
        Timer timer = new Timer("CHECKDEVICE_" + deviceName, true);
        timer.schedule(deviceTimer, timerDelay * 1000);
        return timer;
    }

    public Timer createDownloadTimer(long taskId) {
        if (debugMode()) {
            LOGGER.debug("Timer [DEBUG MODE]: DownloadTimer not created");
            return null;
        }
        DownloadTimer downloadTimer = applicationContext.getBean(DownloadTimer.class);
        downloadTimer.setTaskId(taskId);

        ManagerParam managerParam = managerParamRepository.getByParamName("DOWNLOAD_TIMEOUT");
        if (managerParam == null) {
            LOGGER.error("Agent param with name: DOWNLOAD_TIMEOUT not found");
            return null;
        }
        long timerDelay = Long.parseLong(managerParam.getParamValue());
        Timer timer = new Timer("DOWNLOAD_" + taskId, true);
        timer.schedule(downloadTimer, timerDelay * 1000);
        return timer;
    }

    public Timer createCheckTimer(long taskId) {
        if (debugMode()) {
            LOGGER.debug("Timer [DEBUG MODE]: CheckTaskTimer not created");
            return null;
        }
        CheckTaskTimer checkTaskTimer = applicationContext.getBean(CheckTaskTimer.class);
        checkTaskTimer.setTaskId(taskId);

        ManagerParam managerParam = managerParamRepository.getByParamName("CHECK_TIMEOUT");
        if (managerParam == null) {
            LOGGER.error("Agent param with name: CHECK_TIMEOUT not found");
            return null;
        }
        long timerDelay = Long.parseLong(managerParam.getParamValue());
        Timer timer = new Timer("CHECK_" + taskId, true);
        timer.schedule(checkTaskTimer, timerDelay * 1000);
        return timer;
    }

    public Timer createDeployTimer(long taskId) {
        if (debugMode()) {
            LOGGER.debug("Timer [DEBUG MODE]: DeployTimer not created");
            return null;
        }
        DeployTimer deployTimer = applicationContext.getBean(DeployTimer.class);
        deployTimer.setTaskId(taskId);

        ManagerParam managerParam = managerParamRepository.getByParamName("DEPLOY_TIMEOUT");
        if (managerParam == null) {
            LOGGER.error("Agent param with name: DEPLOY_TIMEOUT not found");
            return null;
        }
        long timerDelay = Long.parseLong(managerParam.getParamValue());
        Timer timer = new Timer("DEPLOY_" + taskId, true);
        timer.schedule(deployTimer, timerDelay * 1000);
        return timer;
    }

    public Timer createStopTimer(long taskId) {
        if (debugMode()) {
            LOGGER.debug("Timer [DEBUG MODE]: StopTimer not created");
            return null;
        }
        StopTimer stopTimer = applicationContext.getBean(StopTimer.class);
        stopTimer.setTaskId(taskId);

        ManagerParam managerParam = managerParamRepository.getByParamName("STOP_TIMEOUT");
        if (managerParam == null) {
            LOGGER.error("Agent param with name: STOP_TIMEOUT not found");
            return null;
        }
        long timerDelay = Long.parseLong(managerParam.getParamValue());
        Timer timer = new Timer("STOP_" + taskId, true);
        timer.schedule(stopTimer, timerDelay * 1000);
        return timer;
    }

    public Timer createCollectTimer(long taskId) {
        if (debugMode()) {
            LOGGER.debug("Timer [DEBUG MODE]: CollectTimer not created");
            return null;
        }
        CollectTimer collectTimer = applicationContext.getBean(CollectTimer.class);
        collectTimer.setTaskId(taskId);

        ManagerParam managerParam = managerParamRepository.getByParamName("COLLECT_TIMEOUT");
        if (managerParam == null) {
            LOGGER.error("Agent param with name: COLLECT_TIMEOUT not found");
            return null;
        }
        long timerDelay = Long.parseLong(managerParam.getParamValue());
        Timer timer = new Timer("COLLECT_" + taskId, true);

        timer.schedule(collectTimer, timerDelay * 1000);
        return timer;
    }

    public Timer createUploadTimer(long taskId) {
        if (debugMode()) {
            LOGGER.debug("Timer [DEBUG MODE]: UploadTimer not created");
            return null;
        }
        UploadTimer uploadTimer = applicationContext.getBean(UploadTimer.class);
        uploadTimer.setTaskId(taskId);

        ManagerParam managerParam = managerParamRepository.getByParamName("UPLOAD_TIMEOUT");
        if (managerParam == null) {
            LOGGER.error("Agent param with name: UPLOAD_TIMEOUT not found");
            return null;
        }
        long timerDelay = Long.parseLong(managerParam.getParamValue());
        Timer timer = new Timer("UPLOAD_" + taskId, true);
        timer.schedule(uploadTimer, timerDelay * 1000);
        return timer;
    }

    public Timer createProgressInfoTimer(long taskId) {
        if (debugMode()) {
            LOGGER.debug("Timer [DEBUG MODE]: ProgressInfoTimer not created");
            return null;
        }
        ProgressInfoTimer progressInfoTimer = applicationContext.getBean(ProgressInfoTimer.class);
        progressInfoTimer.setTaskId(taskId);

        ManagerParam managerParam = managerParamRepository.getByParamName("PROGRESSINFO_TIMEOUT");
        if (managerParam == null) {
            LOGGER.error("Agent param with name: PROGRESSINFO_TIMEOUT not found");
            return null;
        }
        long timerDelay = Long.parseLong(managerParam.getParamValue());
        Timer timer = new Timer("PROGRESSINFO_" + taskId, true);
        timer.schedule(progressInfoTimer, timerDelay * 1000);
        return timer;
    }

    public Timer createRestoreTimer() {
        ManagerParam managerParam = managerParamRepository.getByParamName("RESTORE_TIMEOUT");
        if (managerParam == null) {
            LOGGER.error("Agent param with name: RESTORE_TIMEOUT not found");
            return null;
        }
        long timerDelay = Long.parseLong(managerParam.getParamValue());
        Timer timer = new Timer("RestoreTimer", true);
        RestoreTimer restoreTimer = applicationContext.getBean(RestoreTimer.class);
        timer.schedule(restoreTimer, timerDelay * 1000);
        return timer;
    }

    public Timer createLogCleanerTimer() {
        long timerDelay = 24 * 3600;
        Timer timer = new Timer("LogCleanerTimer", true);
        LogCleanerTimer restoreTimer = applicationContext.getBean(LogCleanerTimer.class);
        timer.schedule(restoreTimer, timerDelay * 1000);
        return timer;
    }

    public Timer createCheckDecisionTimer() {
        Timer timer = new Timer("DecisionCreatorTimer", true);
        CheckDecisionTimer checkDecisionTimer = applicationContext.getBean(CheckDecisionTimer.class);
        timer.schedule(checkDecisionTimer, 3 * 1000);
        return timer;
    }

    public void cancelTimer(Timer timer) {
        if (timer != null) {
            timer.cancel();
        }
    }

    private boolean debugMode() {
        ManagerParam debugModeParam = managerParamRepository.getByParamName("AGENT_DEBUG_MODE");
        if (debugModeParam == null) {
            LOGGER.error("Agent parameter with name: AGENT_DEBUG_MODE not found");
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
