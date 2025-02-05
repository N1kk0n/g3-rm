package g3.rm.resourcemanager.init.services;

import g3.rm.resourcemanager.init.dtos.ResourceManagerParam;
import g3.rm.resourcemanager.init.repositories.state.ParamStateRepository;
import g3.rm.resourcemanager.init.repositories.cache.ParamCacheRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UpdateParametersService {

    private final ParamStateRepository paramStateRepository;
    private final ParamCacheRepository paramCacheRepository;
    private final Logger LOGGER = LogManager.getLogger(UpdateParametersService.class);

    public UpdateParametersService(ParamStateRepository paramStateRepository, ParamCacheRepository paramCacheRepository) {
        this.paramStateRepository = paramStateRepository;
        this.paramCacheRepository = paramCacheRepository;
    }

    private List<ResourceManagerParam> getRemoteParams() {
        return paramStateRepository.getParams();
    }

    private void setLocalParam(ResourceManagerParam remoteParam) {
        paramCacheRepository.addParam(remoteParam.getParamName(), remoteParam.getParamValue());
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
