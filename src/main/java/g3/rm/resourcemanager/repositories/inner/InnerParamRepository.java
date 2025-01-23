package g3.rm.resourcemanager.repositories.inner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class InnerParamRepository {
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public InnerParamRepository(@Qualifier("innerJdbcTemplate") NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public void addParam(String paramName, String paramValue) {
        String sql = """
            insert into resource_manager_param(param_name, param_value)
            values (:paramName, :paramValue)
        """;
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("paramName", paramName)
                .addValue("paramValue", paramValue);
        template.update(sql, sqlParameterSource);
    }

    public void setParam(String paramName, String paramValue) {
        String sql = """
            update resource_manager_param set param_value = :paramValue
            where param_name = :paramName
        """;
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("paramName", paramName)
                .addValue("paramValue", paramValue);
        template.update(sql, sqlParameterSource);
    }

    public String getParamValue(String paramName) {
        String sql = """
            select param_value from resource_manager_param
            where param_name = :paramName
        """;
        SqlParameterSource namedParameters = new MapSqlParameterSource()
                .addValue("paramName", paramName);
        return template.queryForObject(sql, namedParameters, String.class);
    }
}
