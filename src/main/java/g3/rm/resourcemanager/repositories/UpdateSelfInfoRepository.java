package g3.rm.resourcemanager.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class UpdateSelfInfoRepository {
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public UpdateSelfInfoRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public void setManagerOnlineStatus(String managerName, boolean isOnline) {
        String sql = """
                update resource_manager
                set manager_online = :isOnline
                where manager_name = :managerName
                """;
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("isOnline", isOnline)
                .addValue("managerName", managerName);

        template.update(sql, sqlParameterSource);
    }
}
