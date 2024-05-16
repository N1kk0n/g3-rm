package g3.rm.resourcemanager.autorun;

import g3.rm.resourcemanager.services.LogCleanerService;
import g3.rm.resourcemanager.services.ProcessContainerService;
import g3.rm.resourcemanager.services.TimerCreatorService;
import g3.rm.resourcemanager.services.UpdateParametersService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AutorunBean {
    private final String MANAGER_NAME = "manager.name";
    private final String HOST_PORT = "host.port";
    private final String ROOT_CERT = "root.cert";
    private final String USER_CERT = "user.cert";
    private final String USER_KEY = "user.key.pk8";
    @Autowired
    private Environment environment;
    @Autowired
    private ProcessContainerService processContainerService;
    @Autowired
    private LogCleanerService logCleanerService;
    @Autowired
    private UpdateParametersService updateParametersService;
    @Autowired
    private TimerCreatorService timerCreatorService;

    @PostConstruct
    public void init() {
        if (!isParamsCorrect()) {
            System.exit(-1);
        }
        System.out.println("Input parameters has checked. Start Agent configuration update procedure...");
        updateParametersService.updateManagerParams(environment.getProperty(MANAGER_NAME));
        updateParametersService.updateDeviceParams(environment.getProperty(MANAGER_NAME));
        updateParametersService.updateProgramParams(environment.getProperty(MANAGER_NAME));
//        processContainerService.clearTaskProcesses();
//        logCleanerService.cleanOldTaskLogs();
        timerCreatorService.createCheckDecisionTimer();
    }

    private boolean isParamsCorrect() {
        String message = """
                Example: java -jar g3-rm.jar --manager.name=<resource manager name>\\
                 --host.port=<host and port of database>\\
                 --root.cert=<path to root certificate file>\\
                 --user.cert=<path to user certificate file>\\
                 --user.key.pk8=<path to key pk8 file>
                """;
        return isParamCorrect(MANAGER_NAME, message) &&
                isParamCorrect(HOST_PORT, message) &&
                isParamCorrect(ROOT_CERT, message) &&
                isParamCorrect(USER_CERT, message) &&
                isParamCorrect(USER_KEY, message);
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
