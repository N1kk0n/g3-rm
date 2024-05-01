package g3.rm.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import g3.rm.jpa_domain.TaskProcess;

@Repository
public interface TaskProcessRepository extends CrudRepository<TaskProcess, Long>{
    TaskProcess findByStageId(Long stageId);
    Iterable<TaskProcess> findAllByEntityIdAndOperation(Long entityId, String operation);
}
