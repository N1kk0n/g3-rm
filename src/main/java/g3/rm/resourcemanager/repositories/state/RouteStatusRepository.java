package g3.rm.resourcemanager.repositories.state;

import org.apache.logging.log4j.core.util.JsonUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RouteStatusRepository {

    private final NamedParameterJdbcTemplate template;

    public RouteStatusRepository(@Qualifier("stateJdbcTemplate") NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public boolean isRouteNotActive(long routeId) {
        String sql = """
            select status
            from state_schema.route
            where id = :routeId
        """;
        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("routeId", routeId);
        Integer status = template.queryForObject(sql, sqlParameterSource, Integer.class);
        return status == null || status != 1;
    }

    public void setRouteStatus(long routeId, int status) {
        String sql = """
            update state_schema.route
            set status = :status
            where id = :routeId
        """;
        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("status", status)
                .addValue("routeId", routeId);
        template.update(sql, sqlParameterSource);
    }
}
