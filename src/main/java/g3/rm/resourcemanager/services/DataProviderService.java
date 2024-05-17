package g3.rm.resourcemanager.services;

import g3.rm.resourcemanager.dtos.Task;
import g3.rm.resourcemanager.datasources.Ceph;
import g3.rm.resourcemanager.datasources.OracleDB;
import g3.rm.resourcemanager.entities.ManagerParam;
import g3.rm.resourcemanager.repositories.ManagerParamRepository;
import g3.rm.resourcemanager.router.RouterEventPublisher;
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
    private ManagerParamRepository managerParamRepository;
    @Autowired
    private ProcessContainerService processContainerService;
    @Autowired
    private RouterEventPublisher eventPublisher;

    private final Logger LOGGER = LogManager.getLogger("DataProviderService");

    public DataProviderService() {
    }

    public void download(Task task) {
        ManagerParam dataSourceParam = managerParamRepository.getByParamName("DATA_SOURCE");
        if (dataSourceParam == null) {
            LOGGER.error("Agent parameter DATA_SOURCE not found");
            eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", task);
            return;
        }
        String dataSource = dataSourceParam.getParamValue();
        switch (dataSource) {
            case "ceph": {
                Ceph contextBean = context.getBean(Ceph.class);
                contextBean.setTaskObject(task);
                processContainerService.addProcess(contextBean.download(), "DOWNLOAD", task.getTaskId());
                break;
            }
            case "oracle": {
                OracleDB contextBean = context.getBean(OracleDB.class);
                contextBean.setTaskObject(task);
                contextBean.setDataSource(initOracleDataSource());
                processContainerService.addProcess(contextBean.download(), "DOWNLOAD", task.getTaskId());
                break;
            }
            default:
                LOGGER.error("Download error. Wrong DATA_SOURCE value in AgentParams. Must be [ oracle | ceph ]");
                eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", task);
        }
    }

    public void upload(Task task) {
        ManagerParam dataSourceParam = managerParamRepository.getByParamName("DATA_SOURCE");
        if (dataSourceParam == null) {
            LOGGER.error("Agent parameter DATA_SOURCE not found");
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", task);
            return;
        }
        String dataSourceType = dataSourceParam.getParamValue();
        switch (dataSourceType) {
            case "ceph": {
                Ceph contextBean = context.getBean(Ceph.class);
                contextBean.setTaskObject(task);
                processContainerService.addProcess(contextBean.upload(), "UPLOAD", task.getTaskId());
                break;
            }
            case "oracle": {
                OracleDB contextBean = context.getBean(OracleDB.class);
                contextBean.setTaskObject(task);
                contextBean.setDataSource(initOracleDataSource());
                processContainerService.addProcess(contextBean.upload(), "UPLOAD", task.getTaskId());
                break;
            }
            default: 
                LOGGER.error("Upload error. Wrong DATA_SOURCE value in AgentParams. Must be [ oracle | ceph ]");
                eventPublisher.publishTaskEvent("UPLOAD_ERROR", task);
        }
    }

    private DataSource initOracleDataSource() {
        DataSourceBuilder<?> builder = DataSourceBuilder.create();
        builder.type(PGSimpleDataSource.class);
        builder.driverClassName("org.postgresql.Driver");
        builder.url(managerParamRepository.getByParamName("DB_URL").getParamValue());
        builder.username(managerParamRepository.getByParamName("DB_USER").getParamValue());
        builder.password(managerParamRepository.getByParamName("DB_PASSWORD").getParamValue());
        return builder.build();
    }
}
