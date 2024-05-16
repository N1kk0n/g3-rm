package g3.rm.resourcemanager.repositories;

import g3.rm.resourcemanager.dtos.ManagerParam;
import g3.rm.resourcemanager.dtos.DeviceParam;
import g3.rm.resourcemanager.dtos.ProgramParam;
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

    public List<ManagerParam> getManagerParams(String managerName) {
        String sql = """
                select rm.manager_id, param_name, param_value
                from resource_manager_param rmp left join resource_manager rm on rmp.manager_id = rm.manager_id
                where rm.manager_name = :name
                """;
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("name", managerName);

        return template.query(sql, sqlParameterSource, (resultSet, rowNum) -> {
            ManagerParam managerParam = new ManagerParam();
            managerParam.setManagerId(resultSet.getInt("MANAGER_ID"));
            managerParam.setParamName(resultSet.getString("PARAM_NAME"));
            managerParam.setParamValue(resultSet.getString("PARAM_VALUE"));
            return managerParam;
        });
    }

    public List<DeviceParam> getDeviceParams(String managerName) {
        String sql = """
                select d.device_id, device_name,  param_name, param_value
                from resource_manager_device_param rmdp left join resource_manager rm on rmdp.manager_id = rm.manager_id
                                                        left join device d on rmdp.device_id = d.device_id
                where rm.manager_name = :name
                """;
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("name", managerName);

        return template.query(sql, sqlParameterSource, (resultSet, rowNum) -> {
            DeviceParam deviceParam = new DeviceParam();
            deviceParam.setDeviceId(resultSet.getInt("DEVICE_ID"));
            deviceParam.setDeviceName(resultSet.getString("DEVICE_NAME"));
            deviceParam.setParamName(resultSet.getString("PARAM_NAME"));
            deviceParam.setParamValue(resultSet.getString("PARAM_VALUE"));
            return deviceParam;
        });
    }

    public List<ProgramParam> getProgramParams(String managerName) {
        String sql = """
                select program_id, param_name, param_value
                from resource_manager_program_param rmpp left join resource_manager rm on rm.manager_id = rmpp.manager_id
                where rm.manager_name = :name
                """;
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("name", managerName);

        return template.query(sql, sqlParameterSource, (resultSet, rowNum) -> {
            ProgramParam programParam = new ProgramParam();
            programParam.setProgramId(resultSet.getInt("PROGRAM_ID"));
            programParam.setParamName(resultSet.getString("PARAM_NAME"));
            programParam.setParamValue(resultSet.getString("PARAM_VALUE"));
            return programParam;
        });
    }
}
