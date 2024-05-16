package g3.rm.resourcemanager.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import g3.rm.resourcemanager.entities.DeviceParam;

@Repository
public interface DeviceParamRepository extends CrudRepository<DeviceParam, Long>{
    DeviceParam findByDeviceNameAndParamName(String deviceName, String paramName);
}
