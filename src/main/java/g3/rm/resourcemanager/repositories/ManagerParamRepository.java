package g3.rm.resourcemanager.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import g3.rm.resourcemanager.entities.ManagerParam;

@Repository
public interface ManagerParamRepository extends CrudRepository<ManagerParam, Long> {
    ManagerParam getByParamName(String name);
}
