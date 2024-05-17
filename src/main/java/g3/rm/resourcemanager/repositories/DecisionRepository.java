package g3.rm.resourcemanager.repositories;

import g3.rm.resourcemanager.dtos.DecisionItem;
import g3.rm.resourcemanager.dtos.ManagerParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DecisionRepository {
    @Autowired
    private Environment environment;

    private final NamedParameterJdbcTemplate template;

    private final String MANAGER_NAME = "manager.name";

    @Autowired
    public DecisionRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public List<DecisionItem> getDecision() {
        String managerName = environment.getProperty(MANAGER_NAME);
        String sql = """
                select task_id, device_name
                from decision
                where manager_name = :name
                """;
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("name", managerName);

        return template.query(sql, sqlParameterSource, (resultSet, rowNum) -> {
            DecisionItem decisionItem = new DecisionItem();
            decisionItem.setTaskId(resultSet.getLong("TASK_ID"));
            decisionItem.setDeviceName(resultSet.getString("DEVICE_NAME"));
            return decisionItem;
        });
    }
}
