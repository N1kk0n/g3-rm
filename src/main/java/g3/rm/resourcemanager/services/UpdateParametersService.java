package g3.rm.resourcemanager.services;

import g3.rm.resourcemanager.dtos.ManagerParam;
import g3.rm.resourcemanager.dtos.DeviceParam;
import g3.rm.resourcemanager.dtos.ProgramParam;
import g3.rm.resourcemanager.repositories.ManagerParamRepository;
import g3.rm.resourcemanager.repositories.DeviceParamRepository;
import g3.rm.resourcemanager.repositories.InitialParamRepository;
import g3.rm.resourcemanager.repositories.ProgramParamRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UpdateParametersService {
    @Autowired
    private InitialParamRepository initialParamRepository;
    @Autowired
    private ManagerParamRepository managerParamRepository;
    @Autowired
    private DeviceParamRepository deviceParamRepository;
    @Autowired
    private ProgramParamRepository programParamRepository;
    @Autowired
    private Environment environment;

    private final String MANAGER_NAME ="manager.name";
    private final Logger LOGGER = LogManager.getLogger("UpdateParametersService");

    public String updateManagerParams() {
        LOGGER.info("UPDATING: Manager parameters");
        List<ManagerParam> remoteManagerParams = initialParamRepository.getManagerParams(environment.getProperty(MANAGER_NAME));
        if (remoteManagerParams.size() == 0) {
            LOGGER.info("UPDATING: Manager parameters from main database are empty.");
            System.err.println("Manager parameters from main database are empty.");
            return "Update parameters: Manager parameters from main database are empty.";
        }
        System.out.println("Updating manager parameters:");

        for (ManagerParam managerParam : remoteManagerParams) {
            g3.rm.resourcemanager.entities.ManagerParam localParam = managerParamRepository.getByParamName(managerParam.getParamName());
            if (localParam == null) {
                localParam = new g3.rm.resourcemanager.entities.ManagerParam();
                localParam.setParamName(managerParam.getParamName());
            }
            localParam.setParamValue(managerParam.getParamValue());
            managerParamRepository.save(localParam);
            System.out.println("[ NAME: '" + localParam.getParamName() + "', VALUE: '" + localParam.getParamValue() + "' ]");
        }
        return "Update parameters: Success.";
    }

    public void updateManagerParams(String managerName) {
        LOGGER.info("STARTUP UPDATING: Manager parameters");
        List<ManagerParam> remoteManagerParams = initialParamRepository.getManagerParams(managerName);
        if (remoteManagerParams.size() == 0) {
            LOGGER.info("UPDATING: Manager parameters from main database are empty.");
            System.err.println("Manager parameters from main database are empty.");
            return;
        }
        managerParamRepository.deleteAll();
        System.out.println("Updating manager parameters:");

        for (ManagerParam managerParam : remoteManagerParams) {
            g3.rm.resourcemanager.entities.ManagerParam localParam = new g3.rm.resourcemanager.entities.ManagerParam();
            localParam.setParamName(managerParam.getParamName());
            localParam.setParamValue(managerParam.getParamValue());
            managerParamRepository.save(localParam);
            System.out.println("[ NAME: '" + localParam.getParamName() + "', VALUE: '" + localParam.getParamValue() + "' ]");
        }
    }

    public String updateDeviceParams() {
        LOGGER.info("UPDATING: Device parameters");
        List<DeviceParam> remoteDeviceParams =
                initialParamRepository.getDeviceParams(environment.getProperty(MANAGER_NAME));
        if (remoteDeviceParams.size() == 0) {
            LOGGER.info("UPDATING: Device parameters from main database are empty.");
            System.err.println("Device parameters from main database are empty.");
            return "Update device parameters: Device parameters from main database are empty.";
        }
        System.out.println("Updating device parameters:");

        for (DeviceParam deviceParam : remoteDeviceParams) {
            g3.rm.resourcemanager.entities.DeviceParam localParam =
                    deviceParamRepository.findByDeviceNameAndParamName(deviceParam.getDeviceName(), deviceParam.getParamName());
            if (localParam == null) {
                localParam = new g3.rm.resourcemanager.entities.DeviceParam();
                localParam.setDeviceId(deviceParam.getDeviceId());
                localParam.setDeviceName(deviceParam.getDeviceName());
                localParam.setParamName(deviceParam.getParamName());
            }
            localParam.setParamValue(deviceParam.getParamValue());
            deviceParamRepository.save(localParam);
            System.out.println("[ DEVICE ID: " + localParam.getDeviceId() +
                    ", DEVICE NAME: '" + localParam.getDeviceName() +
                    "', PARAMETER NAME: '" + localParam.getParamName() +
                    "', PARAMETER VALUE: '" + localParam.getParamValue() + "' ]");
        }
        return "Update device parameters: Success.";
    }

    public void updateDeviceParams(String managerName) {
        LOGGER.info("STARTUP UPDATING: Device parameters");
        List<DeviceParam> remoteDeviceParams = initialParamRepository.getDeviceParams(managerName);
        if (remoteDeviceParams.size() == 0) {
            LOGGER.info("UPDATING: Device parameters from main database are empty.");
            System.err.println("Device parameters from main database are empty.");
            return;
        }
        deviceParamRepository.deleteAll();
        System.out.println("Updating device parameters:");

        for (DeviceParam deviceParam : remoteDeviceParams) {
            g3.rm.resourcemanager.entities.DeviceParam localParam = new g3.rm.resourcemanager.entities.DeviceParam();
            localParam.setDeviceId(deviceParam.getDeviceId());
            localParam.setDeviceName(deviceParam.getDeviceName());
            localParam.setParamName(deviceParam.getParamName());
            localParam.setParamValue(deviceParam.getParamValue());
            deviceParamRepository.save(localParam);
            System.out.println("[ DEVICE ID: " + localParam.getDeviceId() +
                    ", DEVICE NAME: '" + localParam.getDeviceName() +
                    "', PARAMETER NAME: '" + localParam.getParamName() +
                    "', PARAMETER VALUE: '" + localParam.getParamValue() + "' ]");
        }
    }

    public String updateProgramParams() {
        LOGGER.info("UPDATING: Program parameters");
        List<ProgramParam> remoteProgramParams = initialParamRepository.getProgramParams(environment.getProperty(MANAGER_NAME));
        if (remoteProgramParams.size() == 0) {
            LOGGER.info("UPDATING: Program parameters from main database are empty.");
            System.err.println("Program parameters from main database are empty.");
            return "Updating program parameters: Program parameters from main database are empty.";
        }
        System.out.println("Updating program parameters:");

        for (ProgramParam programParam : remoteProgramParams) {
            g3.rm.resourcemanager.entities.ProgramParam localParam =
                    programParamRepository.findByProgramIdAndParamName(programParam.getProgramId(), programParam.getParamName());
            if (localParam == null) {
                localParam = new g3.rm.resourcemanager.entities.ProgramParam();
                localParam.setProgramId(programParam.getProgramId());
                localParam.setParamName(programParam.getParamName());
            }
            localParam.setParamValue(programParam.getParamValue());
            programParamRepository.save(localParam);
            System.out.println("[ PROGRAM ID: " + localParam.getProgramId() +
                    "', PARAMETER NAME: '" + localParam.getParamName() +
                    "', PARAMETER VALUE: '" + localParam.getParamValue() + "' ]");
        }
        return "Update program parameters: Success.";
    }

    public void updateProgramParams(String ManagerName) {
        LOGGER.info("STARTUP UPDATING: Program parameters");
        List<ProgramParam> remoteProgramParams = initialParamRepository.getProgramParams(ManagerName);
        if (remoteProgramParams.size() == 0) {
            LOGGER.info("UPDATING: Program parameters from main database are empty.");
            System.err.println("Program parameters from main database are empty.");
            return;
        }
        programParamRepository.deleteAll();
        System.out.println("Updating program parameters:");

        for (ProgramParam programParam : remoteProgramParams) {
            g3.rm.resourcemanager.entities.ProgramParam localParam = new g3.rm.resourcemanager.entities.ProgramParam();
            localParam.setProgramId(programParam.getProgramId());
            localParam.setParamName(programParam.getParamName());
            localParam.setParamValue(programParam.getParamValue());
            programParamRepository.save(localParam);
            System.out.println("[ PROGRAM ID: " + localParam.getProgramId() +
                    "', PARAMETER NAME: '" + localParam.getParamName() +
                    "', PARAMETER VALUE: '" + localParam.getParamValue() + "' ]");
        }
    }
}
