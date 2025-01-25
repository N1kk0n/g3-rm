package g3.rm.resourcemanager.services;

import g3.rm.resourcemanager.dtos.ResourceManagerParam;
import g3.rm.resourcemanager.repositories.inner.InnerParamRepository;
import g3.rm.resourcemanager.repositories.state.StateParamRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UpdateParametersService {

    private final StateParamRepository stateParamRepository;
    private final InnerParamRepository innerParamRepository;
    private final Logger LOGGER = LogManager.getLogger(UpdateParametersService.class);

    public UpdateParametersService(StateParamRepository stateParamRepository, InnerParamRepository innerParamRepository) {
        this.stateParamRepository = stateParamRepository;
        this.innerParamRepository = innerParamRepository;
    }

    private List<ResourceManagerParam> getRemoteParams() {
        return stateParamRepository.getParams();
    }

    private void setLocalParam(ResourceManagerParam remoteParam) {
        innerParamRepository.addParam(remoteParam.getParamName(), remoteParam.getParamValue());
    }

    public void updateSelfParams() {
        try {
            for (ResourceManagerParam managerParam : getRemoteParams()) {
                setLocalParam(managerParam);
            }
        } catch (Exception ex) {
            LOGGER.error("Error while init resource manager params. Exception message: ", ex);
            System.exit(1);
        }
    }
}
