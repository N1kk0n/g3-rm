package g3.rm.resourcemanager.repositories.state;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Repository
public class StateRouteRepository {

    private final DataSource dataSource;

    public StateRouteRepository(@Qualifier("stateDataSource") DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean isRouteNotActive(long routeId) {
        String sql = """
            select status
            from state_schema.route
            where id = :routeId
        """;
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
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
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("status", status)
                .addValue("routeId", routeId);
        template.update(sql, sqlParameterSource);
    }

    public long createRoute(int graphId) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(dataSource)
                .withSchemaName("state_schema")
                .withTableName("route")
                .usingGeneratedKeyColumns("id")
                .usingColumns("graph_id", "status");
        Map<String, Object> parameters = new HashMap<>(1);
        parameters.put("graph_id", graphId);
        parameters.put("status", 1);

        Number id = simpleJdbcInsert.executeAndReturnKey(parameters);
        return id.longValue();
    }
}
