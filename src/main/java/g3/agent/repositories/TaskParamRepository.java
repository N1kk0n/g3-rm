package g3.agent.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import g3.agent.jpa_domain.TaskParam;

@Repository
public interface TaskParamRepository extends CrudRepository<TaskParam, Long>{
    boolean existsByProgramId(int programId);
    TaskParam findByProgramIdAndParamName(int programId, String paramName);
}
