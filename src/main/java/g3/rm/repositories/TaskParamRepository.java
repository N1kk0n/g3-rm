package g3.rm.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import g3.rm.jpa_domain.TaskParam;

@Repository
public interface TaskParamRepository extends CrudRepository<TaskParam, Long>{
    boolean existsByProgramId(int programId);
    TaskParam findByProgramIdAndParamName(int programId, String paramName);
}
