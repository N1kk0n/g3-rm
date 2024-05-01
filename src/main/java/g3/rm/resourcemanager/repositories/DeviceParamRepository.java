package g3.rm.resourcemanager.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import g3.rm.resourcemanager.jpa_domain.LogicalDeviceParam;

@Repository
public interface DeviceParamRepository extends CrudRepository<LogicalDeviceParam, Long>{
    LogicalDeviceParam findByDeviceNameAndParamName(String deviceName, String paramName);
}
