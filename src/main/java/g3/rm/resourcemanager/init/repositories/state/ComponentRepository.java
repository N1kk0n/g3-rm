package g3.rm.resourcemanager.init.repositories.state;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ComponentRepository {
    private final NamedParameterJdbcTemplate template;

    public ComponentRepository(@Qualifier("stateJdbcTemplate") NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public void updateComponentAddress(String componentName, String ipAddressPort) {
        String sql = """
            update state_schema.component
            set ip_address_port = :ipAddressPort
            where component_name = :componentName
        """;
        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("componentName", componentName)
                .addValue("ipAddressPort", ipAddressPort);
        template.update(sql, sqlParameterSource);
    }
}
