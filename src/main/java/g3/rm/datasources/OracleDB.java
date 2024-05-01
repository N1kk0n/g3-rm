package g3.rm.datasources;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import g3.rm.data.TaskObject;
import g3.rm.repositories.AgentParamRepository;
import g3.rm.repositories.TaskParamRepository;
import g3.rm.router.RouterEventPublisher;
import g3.rm.services.FileSystemService;
import g3.rm.services.TimerService;
import g3.rm.utils.ArchiveOperations;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.time.Instant;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;

public class OracleDB {
    @Autowired
    private TimerService timerService;
    @Autowired
    private AgentParamRepository agentParamRepository;
    @Autowired
    private TaskParamRepository taskParamRepository;
    @Autowired
    private FileSystemService fileSystemService;
    @Autowired
    private RouterEventPublisher eventPublisher;

    private TaskObject taskObject;
    private DataSource dataSource;

    private final Logger LOGGER = LogManager.getLogger("OracleDB");

    public OracleDB() {
    }

    public TaskObject getTaskObject() {
        return taskObject;
    }

    public void setTaskObject(TaskObject taskObject) {
        this.taskObject = taskObject;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Async
    public CompletableFuture<String> download() {
       Timer timer = timerService.createDownloadTimer(taskObject.getTaskId());
        if (!taskParamRepository.existsByProgramId(taskObject.getProgramId())) {
            LOGGER.error("Indication " + taskObject.getProgramId() + " not found");
            timerService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", taskObject);
            return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
        }

        String homeDir = taskParamRepository.findByProgramIdAndParamName(taskObject.getProgramId(), "HOME").getParamValue();
        String taskDir = homeDir + File.separator + taskObject.getTaskId();

        if (!fileSystemService.taskFolderPrepared(taskObject.getTaskId(), taskObject.getProgramId(), taskObject.getDeviceNameList())) {
            LOGGER.error("Error while prepare task folder: " + taskDir);
            timerService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", taskObject);
            return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
        }

        String archivePath = taskDir + File.separator + taskObject.getTaskId() + ".zip";

        int BUFFER_SIZE = 1048576;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement =
                     connection.prepareStatement("SELECT DATASET FROM DM_TASK_DATA WHERE TASK_ID=?")) {
            preparedStatement.setLong(1, taskObject.getTaskId());
            ResultSet resultSet = preparedStatement.executeQuery();
            Blob blob;
            if (!resultSet.next())
                throw new SQLException("Download data " + taskObject.getTaskId() + ".zip from Oracle is failed. Reason: data not found");
            blob = resultSet.getBlob("DATASET");
            if (blob == null)
                throw new SQLException("Download data " + taskObject.getTaskId() + ".zip from Oracle is failed. Reason: data is empty or null");

            File dataFile = new File(archivePath);
            try (InputStream inputStream = blob.getBinaryStream();
                 OutputStream outputStream = Files.newOutputStream(dataFile.toPath())) {
                int read;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((read = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, read);
                }
            }

            ArchiveOperations operation = new ArchiveOperations();
            int unzipResult = operation.unzip(archivePath, dataFile.getParent());
            if (unzipResult == -1) {
                LOGGER.error("Error while unzip " + archivePath + " after download.");
                eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", taskObject);
                return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
            }
        } catch (SQLException ex) {
            LOGGER.error("Error while download. Message: " + ex.getMessage(), ex);
            timerService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", taskObject);
            return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
        } catch (IOException ex) {
            LOGGER.error("Download was interrupted. Data: " + archivePath, ex);
            eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", taskObject);
            return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
        }
        timerService.cancelTimer(timer);
        updateLastDownload(taskObject.getTaskId());
        eventPublisher.publishTaskEvent("DOWNLOAD_DONE", taskObject);
        return CompletableFuture.completedFuture("DOWNLOAD_DONE");
    }

    @Async
    public CompletableFuture<String> upload() {
        Timer timer = timerService.createUploadTimer(taskObject.getTaskId());
        if (!taskParamRepository.existsByProgramId(taskObject.getProgramId())) {
            LOGGER.error("Indication " + taskObject.getProgramId() + " not found");
            timerService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", taskObject);
            return CompletableFuture.completedFuture("UPLOAD_ERROR");
        }

        String homeDir = taskParamRepository.findByProgramIdAndParamName(taskObject.getProgramId(), "HOME").getParamValue();
        String taskDir = homeDir + File.separator + taskObject.getTaskId();
        String archivePath = taskDir + File.separator + taskObject.getTaskId() + ".zip";

        if (debugMode()) {
            LOGGER.debug("Upload data [DEBUG MODE]: " + archivePath + " to Oracle DB [table: g3_DM.DM_TASK_DATA, task_id: " + taskObject.getTaskId() + "]");
            eventPublisher.publishTaskEvent("UPLOAD_DONE", taskObject);
            return CompletableFuture.completedFuture("UPLOAD_DONE");
        }

        File zipFile = new File(archivePath);
        ArchiveOperations operation = new ArchiveOperations();
        int zipResult = operation.zip(zipFile.getParent() , archivePath);
        if (zipResult == -1) {
            LOGGER.error("Error while zip " + archivePath + " before upload.");
            timerService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", taskObject);
            return CompletableFuture.completedFuture("UPLOAD_ERROR");
        }
        if (!zipFile.exists()) {
            LOGGER.error("File " + archivePath + " not found");
            timerService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", taskObject);
            return CompletableFuture.completedFuture("UPLOAD_ERROR");
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement =
                     connection.prepareStatement("update DM_TASK_DATA set DATASET=? where TASK_ID=?")) {
            try (FileInputStream fileInputStream = new FileInputStream(zipFile)) {
                preparedStatement.setBinaryStream(1, fileInputStream);
                preparedStatement.setLong(2, taskObject.getTaskId());
                preparedStatement.execute();
            }
        } catch (SQLException ex) {
            LOGGER.error("Error while upload. Message: " + ex.getMessage(), ex);
            timerService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", taskObject);
            return CompletableFuture.completedFuture("UPLOAD_ERROR");
        } catch (IOException ex) {
            LOGGER.error("Upload was interrupted. Data: " + archivePath, ex);
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", taskObject);
            return CompletableFuture.completedFuture("UPLOAD_ERROR");
        }
        timerService.cancelTimer(timer);
        updateLastUpload(taskObject.getTaskId());
        eventPublisher.publishTaskEvent("UPLOAD_DONE", taskObject);
        return CompletableFuture.completedFuture("UPLOAD_DONE");
    }

    private void updateLastDownload(long taskId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "UPDATE DM_TASK_DATA SET LAST_DOWNLOAD=? WHERE TASK_ID=?")
        ) {
            preparedStatement.setTimestamp(1, Timestamp.from(Instant.now()));
            preparedStatement.setLong(2, taskId);
            if (preparedStatement.executeUpdate() == 0) {
                LOGGER.error("Update last download time for task ID: " + taskId + " error. Nothing was changed.");
            }
        } catch (SQLException ex) {
            LOGGER.error("Error while update download time stamp.", ex);
        }
    }

    private void updateLastUpload(long taskId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "UPDATE DM_TASK_DATA SET LAST_UPLOAD=? WHERE TASK_ID=?")
        ) {
            preparedStatement.setTimestamp(1, Timestamp.from(Instant.now()));
            preparedStatement.setLong(2, taskId);
            if (preparedStatement.executeUpdate() == 0) {
                LOGGER.error("Update last upload time for task ID: " + taskId + " error. Nothing was changed.");
            }
        } catch (SQLException ex) {
            LOGGER.error("Error while update upload time stamp.", ex);
        }
    }

    private boolean debugMode() {
        String debugMode = agentParamRepository.getByName("AGENT_DEBUG_MODE").getValue();
        debugMode = debugMode.toLowerCase();
        if (debugMode.equals("1") || debugMode.equals("true")) {
            return true;
        }
        return false;
    }
}
