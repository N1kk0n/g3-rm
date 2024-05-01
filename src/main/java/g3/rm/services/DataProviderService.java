package g3.rm.services;

import g3.rm.data.TaskObject;
import g3.rm.datasources.Ceph;
import g3.rm.datasources.OracleDB;
import g3.rm.jpa_domain.AgentParam;
import g3.rm.repositories.AgentParamRepository;
import g3.rm.router.RouterEventPublisher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
public class DataProviderService {
    @Autowired
    private ApplicationContext context;
    @Autowired
    private AgentParamRepository agentParamRepository;
    @Autowired
    private ProcessContainerService processContainerService;
    @Autowired
    private RouterEventPublisher eventPublisher;

    private final Logger LOGGER = LogManager.getLogger("DataProviderService");

    public DataProviderService() {
    }

    public void download(TaskObject taskObject) {
        AgentParam dataSourceParam = agentParamRepository.getByName("DATA_SOURCE");
        if (dataSourceParam == null) {
            LOGGER.error("Agent parameter DATA_SOURCE not found");
            eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", taskObject);
            return;
        }
        String dataSource = dataSourceParam.getValue();
        switch (dataSource) {
            case "ceph": {
                Ceph contextBean = context.getBean(Ceph.class);
                contextBean.setTaskObject(taskObject);
                processContainerService.addProcess(contextBean.download(), "DOWNLOAD", taskObject.getTaskId());
                break;
            }
            case "oracle": {
                OracleDB contextBean = context.getBean(OracleDB.class);
                contextBean.setTaskObject(taskObject);
                contextBean.setDataSource(initOracleDataSource());
                processContainerService.addProcess(contextBean.download(), "DOWNLOAD", taskObject.getTaskId());
                break;
            }
            default:
                LOGGER.error("Download error. Wrong DATA_SOURCE value in AgentParams. Must be [ oracle | ceph ]");
                eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", taskObject);
        }
    }

    public void upload(TaskObject taskObject) {
        AgentParam dataSourceParam = agentParamRepository.getByName("DATA_SOURCE");
        if (dataSourceParam == null) {
            LOGGER.error("Agent parameter DATA_SOURCE not found");
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", taskObject);
            return;
        }
        String dataSourceType = dataSourceParam.getValue();
        switch (dataSourceType) {
            case "ceph": {
                Ceph contextBean = context.getBean(Ceph.class);
                contextBean.setTaskObject(taskObject);
                processContainerService.addProcess(contextBean.upload(), "UPLOAD", taskObject.getTaskId());
                break;
            }
            case "oracle": {
                OracleDB contextBean = context.getBean(OracleDB.class);
                contextBean.setTaskObject(taskObject);
                contextBean.setDataSource(initOracleDataSource());
                processContainerService.addProcess(contextBean.upload(), "UPLOAD", taskObject.getTaskId());
                break;
            }
            default: 
                LOGGER.error("Upload error. Wrong DATA_SOURCE value in AgentParams. Must be [ oracle | ceph ]");
                eventPublisher.publishTaskEvent("UPLOAD_ERROR", taskObject);
        }
    }

    private DataSource initOracleDataSource() {
        DataSourceBuilder<?> builder = DataSourceBuilder.create();
        builder.type(PGSimpleDataSource.class);
        builder.driverClassName("org.postgresql.Driver");
        builder.url(agentParamRepository.getByName("DB_URL").getValue());
        builder.username(agentParamRepository.getByName("DB_USER").getValue());
        builder.password(agentParamRepository.getByName("DB_PASSWORD").getValue());
        return builder.build();
    }
}
