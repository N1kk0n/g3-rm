package g3.agent.autorun;

import g3.agent.services.LogCleanerService;
import g3.agent.services.ProcessContainerService;
import g3.agent.services.UpdateParametersService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AutorunBean {
    @Autowired
    private Environment environment;
    @Autowired
    private ProcessContainerService processContainerService;
    @Autowired
    private LogCleanerService logCleanerService;
    @Autowired
    private UpdateParametersService updateParametersService;

    private final String AGENT_NAME="agent.name";
    private final String CONFIG_DB_URL="config.url";
    private final String CONFIG_DB_USER="config.user";
    private final String CONFIG_DB_PASSWORD="config.password";

    @PostConstruct
    public void init() {
        if (!isParamsCorrect()) {
            System.exit(-1);
        }
        System.out.println("Input parameters has checked. Start Agent configuration update procedure...");
        updateParametersService.updateAgentParams(environment.getProperty(AGENT_NAME));
        updateParametersService.updateLogicalDeviceParams(environment.getProperty(AGENT_NAME));
        updateParametersService.updateTaskParams(environment.getProperty(AGENT_NAME));
        processContainerService.clearTaskProcesses();
        logCleanerService.cleanOldTaskLogs();
    }

    private boolean isParamsCorrect() {
        String message = "Example: \"java -jar resource-agent.jar --agent.name=<name of agent>\\\n" +
                "--config.url=<url to database with config>\\\n " +
                "--config.user=<username for config db>\\\n " +
                "--config.password=<password for config db>\n";
        return isParamCorrect(AGENT_NAME, message) &&
                isParamCorrect(CONFIG_DB_URL, message) &&
                isParamCorrect(CONFIG_DB_USER, message) &&
                isParamCorrect(CONFIG_DB_PASSWORD, message);
    }

    private boolean isParamCorrect(String parameter, String message) {
        if (environment.getProperty(parameter) == null || Objects.requireNonNull(environment.getProperty(parameter)).isEmpty()) {
            System.err.println("Check input parameter: " + parameter + ". Parameter is null or empty");
            System.err.println(message);
            return false;
        }
        return true;
    }
}
