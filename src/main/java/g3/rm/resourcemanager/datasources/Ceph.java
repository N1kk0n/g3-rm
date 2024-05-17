package g3.rm.resourcemanager.datasources;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;
import g3.rm.resourcemanager.dtos.Task;
import g3.rm.resourcemanager.repositories.ManagerParamRepository;
import g3.rm.resourcemanager.repositories.ProgramParamRepository;
import g3.rm.resourcemanager.router.RouterEventPublisher;
import g3.rm.resourcemanager.services.FileSystemService;
import g3.rm.resourcemanager.services.TimerCreatorService;
import g3.rm.resourcemanager.utils.ArchiveOperations;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;

public class Ceph {
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

    private final Logger LOGGER = LogManager.getLogger("Ceph");

    public Ceph() {
    }

    public Task getTaskObject() {
        return task;
    }

    public void setTaskObject(Task task) {
        this.task = task;
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

        String cephHost = managerParamRepository.getByParamName("CEPH_HOST").getParamValue();
        String accessKey = managerParamRepository.getByParamName("CEPH_ACCESS_KEY").getParamValue();
        String secretKey = managerParamRepository.getByParamName("CEPH_SECRET_KEY").getParamValue();

        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        ClientConfiguration clientConf = new ClientConfiguration();
        clientConf.setProtocol(Protocol.HTTP);

        AwsClientBuilder.EndpointConfiguration endpointConf = new AwsClientBuilder.EndpointConfiguration(cephHost, "");
        AmazonS3 conn = AmazonS3ClientBuilder.standard().withClientConfiguration(clientConf)
                .withCredentials(new AWSStaticCredentialsProvider(credentials)).withEndpointConfiguration(endpointConf)
                .build();
        if (conn == null) {
            LOGGER.error("Connection to: " + cephHost + " failed");
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", task);
            return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
        }
        if (!conn.doesBucketExistV2(task.getBucketName())) {
            LOGGER.error("Bucket with name '" + task.getBucketName() + "' is not exist");
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", task);
            return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
        }
        if (!conn.doesObjectExist(task.getBucketName(), task.getObjectName())) {
            LOGGER.error("Object: " + task.getObjectName() + " not exist");
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", task);
            return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
        }

        String archivePath = taskDir + File.separator + task.getTaskId() + ".zip";
        File dataFile = new File(archivePath);
        if (dataFile.exists()) {
            dataFile.delete();
        }
        TransferManager transferManager = null;
        try {
            transferManager = TransferManagerBuilder.standard().withS3Client(conn).build();
            Download download = transferManager.download(task.getBucketName(), task.getObjectName(), dataFile);
            download.waitForCompletion();

            ArchiveOperations operation = new ArchiveOperations();
            int unzipResult = operation.unzip(archivePath, dataFile.getParent());
            if (unzipResult == -1) {
                LOGGER.error("Error while unzip " + archivePath + " after download.");
                timerCreatorService.cancelTimer(timer);
                eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", task);
                return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
            }
        } catch (InterruptedException ex) {
            LOGGER.error("Download was interrupted. Data: " + archivePath, ex);
            eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", task);
            return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
        } finally {
            if (transferManager != null) {
                transferManager.shutdownNow();
            }
        }
        timerCreatorService.cancelTimer(timer);
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
            LOGGER.debug("Upload data [DEBUG MODE]: " + archivePath + " to Ceph [bucket: " + task.getBucketName() +
                                                                              ", object: " + task.getObjectName() +"]");
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

        String cephHost = managerParamRepository.getByParamName("CEPH_HOST").getParamValue();
        String accessKey = managerParamRepository.getByParamName("CEPH_ACCESS_KEY").getParamValue();
        String secretKey = managerParamRepository.getByParamName("CEPH_SECRET_KEY").getParamValue();

        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        ClientConfiguration clientConf = new ClientConfiguration();
        clientConf.setProtocol(Protocol.HTTP);

        AwsClientBuilder.EndpointConfiguration endpointConf = new AwsClientBuilder.EndpointConfiguration(cephHost, "");
        AmazonS3 conn = AmazonS3ClientBuilder.standard().withClientConfiguration(clientConf)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(endpointConf).build();
        if (conn == null) {
            LOGGER.error("Connection to: " + cephHost + " failed");
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", task);
            return CompletableFuture.completedFuture("UPLOAD_ERROR");
        }
        if (!conn.doesBucketExistV2(task.getBucketName())) {
            LOGGER.error("Bucket with name '" + task.getBucketName() + "' is not exist");
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", task);
            return CompletableFuture.completedFuture("UPLOAD_ERROR");
        }
        TransferManager transferManager = null;
        try {
            MultipartFile uploadFile = convertToMultipart(new File(archivePath));

            InputStream uploadInputStream = uploadFile.getInputStream();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(uploadFile.getSize());
            transferManager = TransferManagerBuilder.standard().withS3Client(conn).build();
            Upload upload = transferManager.upload(task.getBucketName(), task.getObjectName(), uploadInputStream, metadata);
            upload.waitForCompletion();
        } catch (IOException ex) {
            LOGGER.error("Error while upload. Data: " + archivePath + ". Error message: " + ex.getMessage(), ex);
            timerCreatorService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", task);
            return CompletableFuture.completedFuture("UPLOAD_ERROR");
        } catch (InterruptedException ex) {
            LOGGER.error("Upload was interrupted. Data: " + archivePath, ex);
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", task);
            return CompletableFuture.completedFuture("UPLOAD_ERROR");
        } finally {
            if (transferManager != null) {
                transferManager.shutdownNow();
            }
        }
        timerCreatorService.cancelTimer(timer);
        eventPublisher.publishTaskEvent("UPLOAD_DONE", task);
        return CompletableFuture.completedFuture("UPLOAD_DONE");
    }

    private MultipartFile convertToMultipart(File file) throws IOException {
        return new MockMultipartFile(file.getName(), file.getName(), MediaType.APPLICATION_OCTET_STREAM.toString(), new FileInputStream(file));
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
