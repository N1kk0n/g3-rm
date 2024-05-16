package g3.rm.resourcemanager.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import g3.rm.resourcemanager.entities.ProgramParam;

@Repository
public interface ProgramParamRepository extends CrudRepository<ProgramParam, Long>{
    boolean existsByProgramId(int programId);
    ProgramParam findByProgramIdAndParamName(int programId, String paramName);
}
