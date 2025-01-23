package g3.rm.resourcemanager.repositories.state;

import g3.rm.resourcemanager.dtos.ResourceManagerParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StateParamRepository {
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public StateParamRepository(@Qualifier("stateJdbcTemplate") NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public List<ResourceManagerParam> getParams() {
        String sql = """
            select param_name, param_value
            from state_schema.resource_manager_param
        """;
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource();

        return template.query(sql, sqlParameterSource, (resultSet, rowNum) -> {
            ResourceManagerParam param = new ResourceManagerParam();
            param.setParamName(resultSet.getString("PARAM_NAME"));
            param.setParamValue(resultSet.getString("PARAM_VALUE"));
            return param;
        });
    }
}
