package g3.rm.resourcemanager.services;

import g3.rm.resourcemanager.entities.ManagerParam;
import g3.rm.resourcemanager.repositories.ManagerParamRepository;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class FtpTransferService {
    @Autowired
    private ManagerParamRepository managerParamRepository;

    private final Logger LOGGER = LogManager.getLogger("FtpTransferService");

    public String uploadExecutionLog(long taskId, long sessionId) {
        ManagerParam ftpLogHostParam = managerParamRepository.getByParamName("FTP_LOG_HOST");
        if (ftpLogHostParam == null) {
            LOGGER.error("Agent parameter FTP_LOG_HOST not found");
            return "Internal error. Check logs for details";
        }
        String ftpHost = ftpLogHostParam.getParamValue();

        ManagerParam ftpUserParam = managerParamRepository.getByParamName("FTP_LOG_USER");
        if (ftpUserParam == null) {
            LOGGER.error("Agent parameter FTP_LOG_USER not found");
            return "Internal error. Check logs for details";
        }
        String ftpUser = ftpUserParam.getParamValue();

        ManagerParam ftpPasswordParam = managerParamRepository.getByParamName("FTP_LOG_PASSWORD");
        if (ftpPasswordParam == null) {
            LOGGER.error("Agent parameter FTP_LOG_PASSWORD not found");
            return "Internal error. Check logs for details";
        }
        String ftpPassword = ftpPasswordParam.getParamValue();

        ManagerParam ftpDirectoryParam = managerParamRepository.getByParamName("FTP_LOG_DIRECTORY");
        if (ftpDirectoryParam == null) {
            LOGGER.error("Agent parameter FTP_LOG_DIRECTORY not found");
            return "Internal error. Check logs for details";
        }
        String ftpDirectory = ftpDirectoryParam.getParamValue();

        ManagerParam taskLogDirParam = managerParamRepository.getByParamName("TASK_LOG_DIR");
        if (taskLogDirParam == null) {
            LOGGER.error("Agent parameter TASK_LOG_DIR not found");
            return "Internal error. Check logs for details";
        }
        String logPath = taskLogDirParam.getParamValue();
        logPath = logPath + File.separator + taskId + File.separator + sessionId + File.separator + "run.log";
        if (!new File(logPath).exists()) {
            LOGGER.error("File " + logPath + " not found");
            return "Internal error. Check logs for details";
        }

        FTPClient ftpClient = null;
        String result = "Internal error. Check logs for details";
        try {
            ftpClient = new FTPClient();
            ftpClient.connect(ftpHost);
            ftpClient.login(ftpUser, ftpPassword);
            ftpClient.changeWorkingDirectory(ftpDirectory);
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            InputStream inputStream = new FileInputStream(logPath);
            String remoteFileName = taskId + "_" + sessionId + "_exec.log";
            result = ftpClient.storeFile(remoteFileName, inputStream) ? "File transferred successfully" : "File transfer ended with error";
        } catch (IOException ex) {
            LOGGER.error("Error while open connection", ex);
        } finally {
            if (ftpClient != null) {
                try {
                    ftpClient.disconnect();
                } catch (IOException ex) {
                    LOGGER.error("Error while close connection", ex);
                }
            }
        }
        return result;
    }
}
