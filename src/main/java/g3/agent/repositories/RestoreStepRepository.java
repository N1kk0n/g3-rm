package g3.agent.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import g3.agent.jpa_domain.RestoreStep;

import java.util.List;

@Repository
public interface RestoreStepRepository extends CrudRepository<RestoreStep, Long>{
    boolean existsByTaskId(String taskId);
    List<RestoreStep> findByOrderById();
}
