package g3.rm.resourcemanager.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SessionRepository {
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public SessionRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public boolean anotherSessionExists(long taskId) {
        return true;
    }
}
