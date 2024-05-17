package g3.rm.resourcemanager.config;

import g3.rm.resourcemanager.timers.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class TimersConfig {
    @Bean
    @Scope("prototype")
    CheckDecisionTimer checkDecisionTimer() {
        return new CheckDecisionTimer();
    }

    @Bean
    @Scope("prototype")
    CheckDeviceTimer checkDeviceTimer() {
        return new CheckDeviceTimer();
    }

    @Bean
    @Scope("prototype")
    CheckTaskTimer checkTaskTimer() {
        return new CheckTaskTimer();
    }

    @Bean
    @Scope("prototype")
    DeployTimer deployTimer() {
        return new DeployTimer();
    }

    @Bean
    @Scope("prototype")
    StopTimer stopTimer() {
        return new StopTimer();
    }

    @Bean
    @Scope("prototype")
    CollectTimer collectTimer() {
        return new CollectTimer();
    }

    @Bean
    @Scope("prototype")
    ProgressInfoTimer progressInfoTimer() {
        return new ProgressInfoTimer();
    }

    @Bean
    @Scope("prototype")
    RestoreTimer restoreTimer() {
        return new RestoreTimer();
    }

    @Bean
    @Scope("prototype")
    LogCleanerTimer logCleanerTimer() {
        return new LogCleanerTimer();
    }

    @Bean
    @Scope("prototype")
    DownloadTimer downloadTimer() {
        return new DownloadTimer();
    }

    @Bean
    @Scope("prototype")
    UploadTimer uploadTimer() {
        return new UploadTimer();
    }

    @Bean
    @Scope("prototype")
    StartTaskCountdown startTaskCountdown() {
        return new StartTaskCountdown();
    }
}
