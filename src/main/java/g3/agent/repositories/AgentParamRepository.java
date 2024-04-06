package g3.agent.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import g3.agent.jpa_domain.AgentParam;

@Repository
public interface AgentParamRepository extends CrudRepository<AgentParam, Long> {
    AgentParam getByName(String name);
}
