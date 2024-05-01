package g3.rm.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import g3.rm.jdbc_domain.AgentParam;
import g3.rm.jdbc_domain.LogicalDeviceParam;
import g3.rm.jdbc_domain.TaskParam;
import g3.rm.repositories.AgentParamRepository;
import g3.rm.repositories.DeviceParamRepository;
import g3.rm.repositories.InitialParamRepository;
import g3.rm.repositories.TaskParamRepository;

import java.util.List;

@Service
public class UpdateParametersService {
    @Autowired
    private InitialParamRepository initialParamRepository;
    @Autowired
    private AgentParamRepository agentParamRepository;
    @Autowired
    private DeviceParamRepository deviceParamRepository;
    @Autowired
    private TaskParamRepository taskParamRepository;
    @Autowired
    private Environment environment;

    private final String AGENT_NAME="agent.name";
    private final Logger LOGGER = LogManager.getLogger("UpdateParametersService");

    public String updateAgentParams() {
        LOGGER.info("UPDATING: Agent parameters");
        List<AgentParam> remoteAgentParams = initialParamRepository.getAgentParams(environment.getProperty(AGENT_NAME));
        if (remoteAgentParams.size() == 0) {
            LOGGER.info("UPDATING: Agent parameters from main database are empty.");
            System.err.println("Agent parameters from main database are empty.");
            return "Update parameters: Agent parameters from main database are empty.";
        }
        System.out.println("Updating Agent parameters:");

        for (AgentParam agentParam : remoteAgentParams) {
            g3.rm.jpa_domain.AgentParam localParam = agentParamRepository.getByName(agentParam.getParamName());
            if (localParam == null) {
                localParam = new g3.rm.jpa_domain.AgentParam();
                localParam.setName(agentParam.getParamName());
            }
            localParam.setValue(agentParam.getParamValue());
            agentParamRepository.save(localParam);
            System.out.println("[ NAME: '" + localParam.getName() + "', VALUE: '" + localParam.getValue() + "' ]");
        }
        return "Update parameters: Success.";
    }

    public void updateAgentParams(String agentName) {
        LOGGER.info("STARTUP UPDATING: Agent parameters");
        List<AgentParam> remoteAgentParams = initialParamRepository.getAgentParams(agentName);
        if (remoteAgentParams.size() == 0) {
            LOGGER.info("UPDATING: Agent parameters from main database are empty.");
            System.err.println("Agent parameters from main database are empty.");
            return;
        }
        agentParamRepository.deleteAll();
        System.out.println("Updating Agent parameters:");

        for (AgentParam agentParam : remoteAgentParams) {
            g3.rm.jpa_domain.AgentParam localParam = new g3.rm.jpa_domain.AgentParam();
            localParam.setName(agentParam.getParamName());
            localParam.setValue(agentParam.getParamValue());
            agentParamRepository.save(localParam);
            System.out.println("[ NAME: '" + localParam.getName() + "', VALUE: '" + localParam.getValue() + "' ]");
        }
    }

    public String updateLogicalDeviceParams() {
        LOGGER.info("UPDATING: Logical device parameters");
        List<LogicalDeviceParam> remoteLogicalDeviceParams =
                initialParamRepository.getLogicalDeviceParams(environment.getProperty(AGENT_NAME));
        if (remoteLogicalDeviceParams.size() == 0) {
            LOGGER.info("UPDATING: Logical device parameters from main database are empty.");
            System.err.println("Logical device parameters from main database are empty.");
            return "Update logical device parameters: Logical device parameters from main database are empty.";
        }
        System.out.println("Updating logical device parameters:");

        for (LogicalDeviceParam logicalDeviceParam : remoteLogicalDeviceParams) {
            g3.rm.jpa_domain.LogicalDeviceParam localParam =
                    deviceParamRepository.findByDeviceNameAndParamName(logicalDeviceParam.getDeviceName(), logicalDeviceParam.getParamName());
            if (localParam == null) {
                localParam = new g3.rm.jpa_domain.LogicalDeviceParam();
                localParam.setDeviceId(logicalDeviceParam.getDeviceId());
                localParam.setDeviceName(logicalDeviceParam.getDeviceName());
                localParam.setParamName(logicalDeviceParam.getParamName());
            }
            localParam.setParamValue(logicalDeviceParam.getParamValue());
            deviceParamRepository.save(localParam);
            System.out.println("[ DEVICE ID: " + localParam.getDeviceId() +
                    ", DEVICE NAME: '" + localParam.getDeviceName() +
                    "', PARAMETER NAME: '" + localParam.getParamName() +
                    "', PARAMETER VALUE: '" + localParam.getParamValue() + "' ]");
        }
        return "Update logical device parameters: Success.";
    }

    public void updateLogicalDeviceParams(String agentName) {
        LOGGER.info("STARTUP UPDATING: Logical device parameters");
        List<LogicalDeviceParam> remoteLogicalDeviceParams = initialParamRepository.getLogicalDeviceParams(agentName);
        if (remoteLogicalDeviceParams.size() == 0) {
            LOGGER.info("UPDATING: Logical device parameters from main database are empty.");
            System.err.println("Logical device parameters from main database are empty.");
            return;
        }
        deviceParamRepository.deleteAll();
        System.out.println("Updating logical device parameters:");

        for (LogicalDeviceParam logicalDeviceParam : remoteLogicalDeviceParams) {
            g3.rm.jpa_domain.LogicalDeviceParam localParam = new g3.rm.jpa_domain.LogicalDeviceParam();
            localParam.setDeviceId(logicalDeviceParam.getDeviceId());
            localParam.setDeviceName(logicalDeviceParam.getDeviceName());
            localParam.setParamName(logicalDeviceParam.getParamName());
            localParam.setParamValue(logicalDeviceParam.getParamValue());
            deviceParamRepository.save(localParam);
            System.out.println("[ DEVICE ID: " + localParam.getDeviceId() +
                    ", DEVICE NAME: '" + localParam.getDeviceName() +
                    "', PARAMETER NAME: '" + localParam.getParamName() +
                    "', PARAMETER VALUE: '" + localParam.getParamValue() + "' ]");
        }
    }

    public String updateTaskParams() {
        LOGGER.info("UPDATING: Task parameters");
        List<TaskParam> remoteTaskParams = initialParamRepository.getTaskParams(environment.getProperty(AGENT_NAME));
        if (remoteTaskParams.size() == 0) {
            LOGGER.info("UPDATING: Task parameters from main database are empty.");
            System.err.println("Task parameters from main database are empty.");
            return "Updating task parameters: Task parameters from main database are empty.";
        }
        System.out.println("Updating task parameters:");

        for (TaskParam taskParam : remoteTaskParams) {
            g3.rm.jpa_domain.TaskParam localParam =
                    taskParamRepository.findByProgramIdAndParamName(taskParam.getProgramId(), taskParam.getParamName());
            if (localParam == null) {
                localParam = new g3.rm.jpa_domain.TaskParam();
                localParam.setProgramId(taskParam.getProgramId());
                localParam.setParamName(taskParam.getParamName());
            }
            localParam.setParamValue(taskParam.getParamValue());
            taskParamRepository.save(localParam);
            System.out.println("[ PROGRAM ID: " + localParam.getProgramId() +
                    "', PARAMETER NAME: '" + localParam.getParamName() +
                    "', PARAMETER VALUE: '" + localParam.getParamValue() + "' ]");
        }
        return "Update task parameters: Success.";
    }

    public void updateTaskParams(String agentName) {
        LOGGER.info("STARTUP UPDATING: Task parameters");
        List<TaskParam> remoteTaskParams = initialParamRepository.getTaskParams(agentName);
        if (remoteTaskParams.size() == 0) {
            LOGGER.info("UPDATING: Task parameters from main database are empty.");
            System.err.println("Task parameters from main database are empty.");
            return;
        }
        taskParamRepository.deleteAll();
        System.out.println("Updating task parameters:");

        for (TaskParam taskParam : remoteTaskParams) {
            g3.rm.jpa_domain.TaskParam localParam = new g3.rm.jpa_domain.TaskParam();
            localParam.setProgramId(taskParam.getProgramId());
            localParam.setParamName(taskParam.getParamName());
            localParam.setParamValue(taskParam.getParamValue());
            taskParamRepository.save(localParam);
            System.out.println("[ PROGRAM ID: " + localParam.getProgramId() +
                    "', PARAMETER NAME: '" + localParam.getParamName() +
                    "', PARAMETER VALUE: '" + localParam.getParamValue() + "' ]");
        }
    }
}
