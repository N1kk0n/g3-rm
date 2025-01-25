package g3.rm.resourcemanager.repositories.inner;

import g3.rm.resourcemanager.dtos.NextRouteVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RouterRepository {
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public RouterRepository(@Qualifier("innerJdbcTemplate") NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public NextRouteVertex route() {
        String sql = """
                """;
        return new NextRouteVertex();
    }
}
