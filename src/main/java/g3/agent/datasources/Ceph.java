package g3.agent.datasources;

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
import g3.agent.data.TaskObject;
import g3.agent.repositories.AgentParamRepository;
import g3.agent.repositories.TaskParamRepository;
import g3.agent.router.RouterEventPublisher;
import g3.agent.services.FileSystemService;
import g3.agent.services.TimerService;
import g3.agent.utils.ArchiveOperations;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;

public class Ceph {
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

    private final Logger LOGGER = LogManager.getLogger("Ceph");

    public Ceph() {
    }

    public TaskObject getTaskObject() {
        return taskObject;
    }

    public void setTaskObject(TaskObject taskObject) {
        this.taskObject = taskObject;
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

        String cephHost = agentParamRepository.getByName("CEPH_HOST").getValue();
        String accessKey = agentParamRepository.getByName("CEPH_ACCESS_KEY").getValue();
        String secretKey = agentParamRepository.getByName("CEPH_SECRET_KEY").getValue();

        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        ClientConfiguration clientConf = new ClientConfiguration();
        clientConf.setProtocol(Protocol.HTTP);

        AwsClientBuilder.EndpointConfiguration endpointConf = new AwsClientBuilder.EndpointConfiguration(cephHost, "");
        AmazonS3 conn = AmazonS3ClientBuilder.standard().withClientConfiguration(clientConf)
                .withCredentials(new AWSStaticCredentialsProvider(credentials)).withEndpointConfiguration(endpointConf)
                .build();
        if (conn == null) {
            LOGGER.error("Connection to: " + cephHost + " failed");
            timerService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", taskObject);
            return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
        }
        if (!conn.doesBucketExistV2(taskObject.getBucketName())) {
            LOGGER.error("Bucket with name '" + taskObject.getBucketName() + "' is not exist");
            timerService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", taskObject);
            return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
        }
        if (!conn.doesObjectExist(taskObject.getBucketName(), taskObject.getObjectName())) {
            LOGGER.error("Object: " + taskObject.getObjectName() + " not exist");
            timerService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", taskObject);
            return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
        }

        String archivePath = taskDir + File.separator + taskObject.getTaskId() + ".zip";
        File dataFile = new File(archivePath);
        if (dataFile.exists()) {
            dataFile.delete();
        }
        TransferManager transferManager = null;
        try {
            transferManager = TransferManagerBuilder.standard().withS3Client(conn).build();
            Download download = transferManager.download(taskObject.getBucketName(), taskObject.getObjectName(), dataFile);
            download.waitForCompletion();

            ArchiveOperations operation = new ArchiveOperations();
            int unzipResult = operation.unzip(archivePath, dataFile.getParent());
            if (unzipResult == -1) {
                LOGGER.error("Error while unzip " + archivePath + " after download.");
                timerService.cancelTimer(timer);
                eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", taskObject);
                return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
            }
        } catch (InterruptedException ex) {
            LOGGER.error("Download was interrupted. Data: " + archivePath, ex);
            eventPublisher.publishTaskEvent("DOWNLOAD_ERROR", taskObject);
            return CompletableFuture.completedFuture("DOWNLOAD_ERROR");
        } finally {
            if (transferManager != null) {
                transferManager.shutdownNow();
            }
        }
        timerService.cancelTimer(timer);
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
            LOGGER.debug("Upload data [DEBUG MODE]: " + archivePath + " to Ceph [bucket: " + taskObject.getBucketName() + 
                                                                              ", object: " + taskObject.getObjectName() +"]");
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

        String cephHost = agentParamRepository.getByName("CEPH_HOST").getValue();
        String accessKey = agentParamRepository.getByName("CEPH_ACCESS_KEY").getValue();
        String secretKey = agentParamRepository.getByName("CEPH_SECRET_KEY").getValue();

        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        ClientConfiguration clientConf = new ClientConfiguration();
        clientConf.setProtocol(Protocol.HTTP);

        AwsClientBuilder.EndpointConfiguration endpointConf = new AwsClientBuilder.EndpointConfiguration(cephHost, "");
        AmazonS3 conn = AmazonS3ClientBuilder.standard().withClientConfiguration(clientConf)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(endpointConf).build();
        if (conn == null) {
            LOGGER.error("Connection to: " + cephHost + " failed");
            timerService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", taskObject);
            return CompletableFuture.completedFuture("UPLOAD_ERROR");
        }
        if (!conn.doesBucketExistV2(taskObject.getBucketName())) {
            LOGGER.error("Bucket with name '" + taskObject.getBucketName() + "' is not exist");
            timerService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", taskObject);
            return CompletableFuture.completedFuture("UPLOAD_ERROR");
        }
        TransferManager transferManager = null;
        try {
            MultipartFile uploadFile = convertToMultipart(new File(archivePath));

            InputStream uploadInputStream = uploadFile.getInputStream();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(uploadFile.getSize());
            transferManager = TransferManagerBuilder.standard().withS3Client(conn).build();
            Upload upload = transferManager.upload(taskObject.getBucketName(), taskObject.getObjectName(), uploadInputStream, metadata);
            upload.waitForCompletion();
        } catch (IOException ex) {
            LOGGER.error("Error while upload. Data: " + archivePath + ". Error message: " + ex.getMessage(), ex);
            timerService.cancelTimer(timer);
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", taskObject);
            return CompletableFuture.completedFuture("UPLOAD_ERROR");
        } catch (InterruptedException ex) {
            LOGGER.error("Upload was interrupted. Data: " + archivePath, ex);
            eventPublisher.publishTaskEvent("UPLOAD_ERROR", taskObject);
            return CompletableFuture.completedFuture("UPLOAD_ERROR");
        } finally {
            if (transferManager != null) {
                transferManager.shutdownNow();
            }
        }
        timerService.cancelTimer(timer);
        eventPublisher.publishTaskEvent("UPLOAD_DONE", taskObject);
        return CompletableFuture.completedFuture("UPLOAD_DONE");
    }

    private MultipartFile convertToMultipart(File file) throws IOException {
        return new MockMultipartFile(file.getName(), file.getName(), MediaType.APPLICATION_OCTET_STREAM.toString(), new FileInputStream(file));
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
