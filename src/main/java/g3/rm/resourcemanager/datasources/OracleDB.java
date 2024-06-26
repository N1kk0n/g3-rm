package g3.rm.resourcemanager.datasources;

import g3.rm.resourcemanager.repositories.ManagerParamRepository;
import g3.rm.resourcemanager.repositories.ProgramParamRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import g3.rm.resourcemanager.dtos.Task;
import g3.rm.resourcemanager.router.RouterEventPublisher;
import g3.rm.resourcemanager.services.FileSystemService;
import g3.rm.resourcemanager.services.TimerCreatorService;
import g3.rm.resourcemanager.utils.ArchiveOperations;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.time.Instant;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;

public class OracleDB {
    @Autowired
    private TimerCreatorService timerCreatorService;
    @Autowired
    private ManagerParamRepository managerParamRepository;
    @Autowired
    private ProgramParamRepository programParamRepository;
    @Autowired
    private FileSystemService fileSystemService;
    @Autowired
    private RouterEventPublisher eventPublisher;

    private Task task;
    private DataSource dataSource;

    private final Logger LOGGER = LogManager.getLogger("OracleDB");

    public OracleDB() {
    }

    public Task getTaskObject() {
        return task;
    }

    public void setTaskObject(Task task) {
        this.task = task;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Async
    public CompletableFuture<String> download() {
       Timer timer = timerCreatorService.createDownloadTimer(task.getTaskId());
        if (!programParamRepository.existsByProgramId(task.getProgramId())) {
            LOGGER.error("Indication " + task.getProgramId() + " not found");
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", task);
            return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
        }

        String homeDir = programParamRepository.findByProgramIdAndParamName(task.getProgramId(), "HOME").getParamValue();
        String taskDir = homeDir + File.separator + task.getTaskId();

        if (!fileSystemService.taskFolderPrepared(task.getTaskId(), task.getProgramId(), task.getDeviceNameList())) {
            LOGGER.error("Error while prepare task folder: " + taskDir);
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", task);
            return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
        }

        String archivePath = taskDir + File.separator + task.getTaskId() + ".zip";

        int BUFFER_SIZE = 1048576;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement =
                     connection.prepareStatement("SELECT DATASET FROM DM_TASK_DATA WHERE TASK_ID=?")) {
            preparedStatement.setLong(1, task.getTaskId());
            ResultSet resultSet = preparedStatement.executeQuery();
            Blob blob;
            if (!resultSet.next())
                throw new SQLException("Download data " + task.getTaskId() + ".zip from Oracle is failed. Reason: data not found");
            blob = resultSet.getBlob("DATASET");
            if (blob == null)
                throw new SQLException("Download data " + task.getTaskId() + ".zip from Oracle is failed. Reason: data is empty or null");

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
                eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", task);
                return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
            }
        } catch (SQLException ex) {
            LOGGER.error("Error while download. Message: " + ex.getMessage(), ex);
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", task);
            return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
        } catch (IOException ex) {
            LOGGER.error("Download was interrupted. Data: " + archivePath, ex);
            eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", task);
            return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
        }
        timerCreatorService.cancelTimer(timer);
        updateLastDownload(task.getTaskId());
        eventPublisher.publishTaskEvent("DOWNLOAD_DONE", task);
        return CompletableFuture.completedFuture("DOWNLOAD_DONE");
    }

    @Async
    public CompletableFuture<String> upload() {
        Timer timer = timerCreatorService.createUploadTimer(task.getTaskId());
        if (!programParamRepository.existsByProgramId(task.getProgramId())) {
            LOGGER.error("Indication " + task.getProgramId() + " not found");
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", task);
            return CompletableFuture.completedFuture("UPLOAD_ERROR");
        }

        String homeDir = programParamRepository.findByProgramIdAndParamName(task.getProgramId(), "HOME").getParamValue();
        String taskDir = homeDir + File.separator + task.getTaskId();
        String archivePath = taskDir + File.separator + task.getTaskId() + ".zip";

        if (debugMode()) {
            LOGGER.debug("Upload data [DEBUG MODE]: " + archivePath + " to Oracle DB [table: g3_DM.DM_TASK_DATA, task_id: " + task.getTaskId() + "]");
            eventPublisher.publishTaskEvent("UPLOAD_DONE", task);
            return CompletableFuture.completedFuture("UPLOAD_DONE");
        }

        File zipFile = new File(archivePath);
        ArchiveOperations operation = new ArchiveOperations();
        int zipResult = operation.zip(zipFile.getParent() , archivePath);
        if (zipResult == -1) {
            LOGGER.error("Error while zip " + archivePath + " before upload.");
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", task);
            return CompletableFuture.completedFuture("UPLOAD_ERROR");
        }
        if (!zipFile.exists()) {
            LOGGER.error("File " + archivePath + " not found");
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", task);
            return CompletableFuture.completedFuture("UPLOAD_ERROR");
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement =
                     connection.prepareStatement("update DM_TASK_DATA set DATASET=? where TASK_ID=?")) {
            try (FileInputStream fileInputStream = new FileInputStream(zipFile)) {
                preparedStatement.setBinaryStream(1, fileInputStream);
                preparedStatement.setLong(2, task.getTaskId());
                preparedStatement.execute();
            }
        } catch (SQLException ex) {
            LOGGER.error("Error while upload. Message: " + ex.getMessage(), ex);
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", task);
            return CompletableFuture.completedFuture("UPLOAD_ERROR");
        } catch (IOException ex) {
            LOGGER.error("Upload was interrupted. Data: " + archivePath, ex);
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", task);
            return CompletableFuture.completedFuture("UPLOAD_ERROR");
        }
        timerCreatorService.cancelTimer(timer);
        updateLastUpload(task.getTaskId());
        eventPublisher.publishTaskEvent("UPLOAD_DONE", task);
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
        String debugMode = managerParamRepository.getByParamName("AGENT_DEBUG_MODE").getParamValue();
        debugMode = debugMode.toLowerCase();
        if (debugMode.equals("1") || debugMode.equals("true")) {
            return true;
        }
        return false;
    }
}
