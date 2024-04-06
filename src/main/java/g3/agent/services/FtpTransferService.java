package g3.agent.services;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import g3.agent.jpa_domain.AgentParam;
import g3.agent.repositories.AgentParamRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class FtpTransferService {
    @Autowired
    private AgentParamRepository agentParamRepository;

    private final Logger LOGGER = LogManager.getLogger("FtpTransferService");

    public String uploadExecutionLog(long taskId, long sessionId) {
        AgentParam ftpLogHostParam = agentParamRepository.getByName("FTP_LOG_HOST");
        if (ftpLogHostParam == null) {
            LOGGER.error("Agent parameter FTP_LOG_HOST not found");
            return "Internal error. Check logs for details";
        }
        String ftpHost = ftpLogHostParam.getValue();

        AgentParam ftpUserParam = agentParamRepository.getByName("FTP_LOG_USER");
        if (ftpUserParam == null) {
            LOGGER.error("Agent parameter FTP_LOG_USER not found");
            return "Internal error. Check logs for details";
        }
        String ftpUser = ftpUserParam.getValue();

        AgentParam ftpPasswordParam = agentParamRepository.getByName("FTP_LOG_PASSWORD");
        if (ftpPasswordParam == null) {
            LOGGER.error("Agent parameter FTP_LOG_PASSWORD not found");
            return "Internal error. Check logs for details";
        }
        String ftpPassword = ftpPasswordParam.getValue();

        AgentParam ftpDirectoryParam = agentParamRepository.getByName("FTP_LOG_DIRECTORY");
        if (ftpDirectoryParam == null) {
            LOGGER.error("Agent parameter FTP_LOG_DIRECTORY not found");
            return "Internal error. Check logs for details";
        }
        String ftpDirectory = ftpDirectoryParam.getValue();

        AgentParam taskLogDirParam = agentParamRepository.getByName("TASK_LOG_DIR");
        if (taskLogDirParam == null) {
            LOGGER.error("Agent parameter TASK_LOG_DIR not found");
            return "Internal error. Check logs for details";
        }
        String logPath = taskLogDirParam.getValue();
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
