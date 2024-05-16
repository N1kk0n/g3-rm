package g3.rm.resourcemanager.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import g3.rm.resourcemanager.entities.RestoreStep;

import java.util.List;

@Repository
public interface RestoreStepRepository extends CrudRepository<RestoreStep, Long>{
    List<RestoreStep> findByOrderById();
}
