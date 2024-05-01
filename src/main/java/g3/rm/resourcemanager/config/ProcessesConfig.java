package g3.rm.resourcemanager.config;

import g3.rm.resourcemanager.processes.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import g3.rm.processes.*;

@Configuration
public class ProcessesConfig {
    @Bean
    @Scope("prototype")
    CheckDevice checkDevice() {
        return new CheckDevice();
    }

    @Bean
    @Scope("prototype")
    CheckTask checkTask() {
        return new CheckTask();
    }

    @Bean
    @Scope("prototype")
    Deploy deploy() {
        return new Deploy();
    }

    @Bean
    @Scope("prototype")
    Run run() {
        return new Run();
    }

    @Bean
    @Scope("prototype")
    Stop stop() {
        return new Stop();
    }

    @Bean
    @Scope("prototype")
    Collect collect() {
        return new Collect();
    }

    @Bean
    @Scope("prototype")
    ProgressInfo progressInfo() {
        return new ProgressInfo();
    }

    @Bean
    @Scope("prototype")
    FinalProgressInfo finalProgressInfo() {
        return new FinalProgressInfo();
    }
}
