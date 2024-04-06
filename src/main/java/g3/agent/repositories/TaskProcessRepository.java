package g3.agent.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import g3.agent.jpa_domain.TaskProcess;

@Repository
public interface TaskProcessRepository extends CrudRepository<TaskProcess, Long>{
    TaskProcess findByStageId(Long stageId);
    Iterable<TaskProcess> findAllByEntityIdAndOperation(Long entityId, String operation);
}
