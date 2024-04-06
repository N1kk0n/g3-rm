package g3.agent.repositories;

import g3.agent.jdbc_domain.AgentParam;
import g3.agent.jdbc_domain.LogicalDeviceParam;
import g3.agent.jdbc_domain.TaskParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InitialParamRepository {
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public InitialParamRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public List<AgentParam> getAgentParams(String agentName) {
        String sql = "select AGENT_ID, PARAM_NAME, PARAM_VALUE " +
                "from AGENT_PARAM ap left join AGENT a on ap.AGENT_ID = a.ID " +
                "where a.CAPTION = :caption";
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("caption", agentName);

        return template.query(sql, sqlParameterSource, (resultSet, rowNum) -> {
            AgentParam agentParam = new AgentParam();
            agentParam.setAgentId(resultSet.getLong("AGENT_ID"));
            agentParam.setParamName(resultSet.getString("PARAM_NAME"));
            agentParam.setParamValue(resultSet.getString("PARAM_VALUE"));
            return agentParam;
        });
    }

    public List<LogicalDeviceParam> getLogicalDeviceParams(String agentName) {
        String sql = "select DEVICE_ID, DEVICE_NAME, PARAM_NAME, PARAM_VALUE " +
                "from AGENT_LOGICAL_DEVICE_PARAM aldp left join AGENT a on aldp.AGENT_ID = a.ID " +
                "where a.CAPTION = :caption";
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("caption", agentName);

        return template.query(sql, sqlParameterSource, (resultSet, rowNum) -> {
            LogicalDeviceParam logicalDeviceParam = new LogicalDeviceParam();
            logicalDeviceParam.setDeviceId(resultSet.getInt("DEVICE_ID"));
            logicalDeviceParam.setDeviceName(resultSet.getString("DEVICE_NAME"));
            logicalDeviceParam.setParamName(resultSet.getString("PARAM_NAME"));
            logicalDeviceParam.setParamValue(resultSet.getString("PARAM_VALUE"));
            return logicalDeviceParam;
        });
    }

    public List<TaskParam> getTaskParams(String agentName) {
        String sql = "select PROGRAM_ID, PARAM_NAME, PARAM_VALUE " +
                "from AGENT_TASK_PARAM atp left join AGENT a on atp.AGENT_ID = a.ID " +
                "where a.CAPTION = :caption";
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("caption", agentName);

        return template.query(sql, sqlParameterSource, (resultSet, rowNum) -> {
            TaskParam taskParam = new TaskParam();
            taskParam.setProgramId(resultSet.getInt("PROGRAM_ID"));
            taskParam.setParamName(resultSet.getString("PARAM_NAME"));
            taskParam.setParamValue(resultSet.getString("PARAM_VALUE"));
            return taskParam;
        });
    }
}
