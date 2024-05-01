package g3.rm.resourcemanager.services;

import g3.rm.resourcemanager.jpa_domain.AgentParam;
import g3.rm.resourcemanager.repositories.AgentParamRepository;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.sql.*;

@Service
public class LoggerService {
    @Autowired
    private AgentParamRepository agentParamRepository;

    private final Logger LOGGER = LogManager.getLogger("LoggerService");

    public String getPage(long taskId, long sessionId, int pageNumber, int pageSize) {
        AgentParam agentParam = agentParamRepository.getByName("TASK_LOG_DIR");
        if (agentParam == null) {
            LOGGER.error("Agent parameter TASK_LOG_DIR not found");
            return "Internal error. Check logs for details";
        }
        String logPath = agentParam.getValue();
        logPath = logPath + File.separator + taskId + File.separator + sessionId + File.separator + "run.log";

        File log = new File(logPath);
        if (!log.exists()) {
            return "File " + logPath + " not found";
        }
        int from = pageNumber * pageSize;
        int to = from + pageSize;
        int lineNumber = -1;
        JsonObjectBuilder builder = Json.createObjectBuilder();
        try {
            FileInputStream fileInputStream = new FileInputStream(log);
            CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
            decoder.onMalformedInput(CodingErrorAction.IGNORE);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, decoder);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            while (line != null) {
                lineNumber++;
                if (lineNumber >= from && lineNumber <= to) {
                    stringBuilder.append(line + "\n");
                }
                line = bufferedReader.readLine();
            }
            int totalPages = lineNumber / pageSize + 1;

            builder.add("total", totalPages);
            builder.add("text", stringBuilder.toString());
            bufferedReader.close();
            inputStreamReader.close();
            fileInputStream.close();

            return builder.build().toString();
        } catch (IOException ex) {
            LOGGER.error("Error while get page: " + ex.getMessage(), ex);
            builder.add("total", -1);
            builder.add("text", "Error: " + ex.getMessage());
            return builder.build().toString();
        }
    }

    public void saveLog(long taskId, long sessionId, String operation) {
        if (debugMode()) {
            LOGGER.info("Saving log [DEBUG MODE] (no actions): " + taskId + ", " + operation);
            return;
        } else {
            LOGGER.info("Saving log: " + taskId + ", " + operation);
        }

        AgentParam agentParam = agentParamRepository.getByName("TASK_LOG_DIR");
        if (agentParam == null) {
            LOGGER.error("Agent parameter TASK_LOG_DIR not found");
            return;
        }
        String logPath = agentParam.getValue();

        switch (operation) {
            case "CHECK":
                logPath = logPath + File.separator + taskId + File.separator + sessionId + File.separator + "check.log";
                break;
            case "DEPLOY":
                logPath = logPath + File.separator + taskId + File.separator + sessionId + File.separator + "deploy.log";
                break;
            case "RUN":
                logPath = logPath + File.separator + taskId + File.separator + sessionId + File.separator + "run.log";
                break;
            case "STOP":
                logPath = logPath + File.separator + taskId + File.separator + sessionId + File.separator + "stop.log";
                break;
            case "COLLECT":
                logPath = logPath + File.separator + taskId + File.separator + sessionId + File.separator + "collect.log";
                break;
        }
        long stageId = getStageId(taskId, sessionId, operation);
        if (stageId == -1) {
            LOGGER.error("Error while get stage ID: ID for operation: " + operation + " session: " + sessionId + " task: " + taskId + " not found");
            return;
        }
        String stageLog = "";
        if (operation.equals("RUN")) {
            stageLog = getRunLogPart(logPath);
            createProgramOutputFile(logPath);
        } else {
            stageLog = getFileContent(logPath);
        }

        DataSource dataSource = initStorageDataSource();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement operationLogStatement =
                     connection.prepareStatement("INSERT INTO DM_SESSION_LOG (STAGE_ID, STAGE_LOG, LOG_DATE) VALUES (?, ?, ?)");
             PreparedStatement execLogStatement =
                     connection.prepareStatement("INSERT INTO DM_TASK_LOG (SES_ID, SESSION_LOG, LOG_DATE) VALUES (?, ?, ?)")
        ) {
            operationLogStatement.setLong(1, stageId);
            operationLogStatement.setString(2, stageLog);
            operationLogStatement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            operationLogStatement.execute();

            if (operation.equals("RUN")) {
                String execLogPath = logPath.replace("run", "exec");
                File execLog = new File(execLogPath);

                Blob blob = connection.createBlob();
                FileInputStream fileInputStream = new FileInputStream(execLog);
                OutputStream outputStream = blob.setBinaryStream(1);
                int readied = -1;
                while ((readied = fileInputStream.read()) != -1) {
                    outputStream.write(readied);
                }
                execLogStatement.setLong(1, sessionId);
                execLogStatement.setBlob(2, blob);
                execLogStatement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                execLogStatement.execute();
                LOGGER.info("Program output log: " + taskId + " saved successfully");
            }
            LOGGER.info("Log: " + taskId + ", " + operation + " saved successfully");
        } catch (IOException | SQLException ex) {
            LOGGER.error("Error save stage log. Message: " + ex.getMessage(), ex);
        }
    }

    private long getStageId(long taskId, long sessionId, String operation) {
        int operationId = 0;
        switch (operation) {
            case "CHECK":
                operationId = 13;
                break;
            case "DEPLOY":
                operationId = 10;
                break;
            case "RUN":
                operationId = 11;
                break;
            case "STOP":
                operationId = 14;
                break;
            case "COLLECT":
                operationId = 12;
                break;
        }

        DataSource dataSource = initStageInfoDataSource();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement =
                     connection.prepareStatement("select STAGE_ID from SM_SESSION ses left join SM_STAGE stg on ses.SES_ID = stg.SES_ID where OPERATION_ID=? and ses.SES_ID=? and TASK_ID=? order by STAGE_ID desc")
        ) {
            preparedStatement.setInt(1, operationId);
            preparedStatement.setLong(2, sessionId);
            preparedStatement.setLong(3, taskId);

            int numberOfAttempts = 3;
            while (numberOfAttempts > 0 ) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (!resultSet.next())
                        throw new SQLException("Stage ID not found");
                    return resultSet.getLong("STAGE_ID");
                } catch (SQLException e) {
                    LOGGER.error("Error while get stage ID (Attempt: " + (4 - numberOfAttempts) + "): ID for operation: " + operation + " session: " + sessionId + " task: " + taskId + " not found.");
                    numberOfAttempts--;
                    //wait 5 seconds if it is another attempt
                    if (numberOfAttempts != 0) {
                        Thread.sleep(5000);
                    }
                }
            }
        } catch (SQLException | InterruptedException ex) {
            LOGGER.error("Error while get stage ID. Message: " + ex.getMessage(), ex);
        }
        return -1;
    }

    private String getFileContent(String filePath) {
        FileInputStream fileInputStream;
        String content;
        try {
            fileInputStream = new FileInputStream(filePath);
            CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
            decoder.onMalformedInput(CodingErrorAction.IGNORE);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, decoder);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            while (line != null) {
                stringBuilder.append(line + "\n");
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
            content = stringBuilder.toString();
        } catch (IOException ex) {
            LOGGER.error("Error while get file content. Message: " + ex.getMessage(), ex);
            content = "";
        }
        return content;
    }

    private String getRunLogPart(String filePath) {
        final String START_PROGRAM_LABEL = "#START_PROGRAM_LABEL";
        final String END_PROGRAM_LABEL = "#END_PROGRAM_LABEL";
        FileInputStream fileInputStream;
        String content;
        try {
            fileInputStream = new FileInputStream(new File(filePath));
            CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
            decoder.onMalformedInput(CodingErrorAction.IGNORE);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, decoder);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            boolean writeFlag = true;
            while (line != null) {
                if (line.contains(START_PROGRAM_LABEL)) {
                    writeFlag = false;
                    line = bufferedReader.readLine();
                    continue;
                }
                if (line.contains(END_PROGRAM_LABEL)) {
                    writeFlag = true;
                    line = bufferedReader.readLine();
                    continue;
                }
                if (writeFlag)
                    stringBuilder.append(line + "\n");
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
            content = stringBuilder.toString();
        } catch (IOException ex) {
            LOGGER.error("Error while get part of run.log content. Message: " + ex.getMessage(), ex);
            content = "";
        }
        return content;
    }

    private void createProgramOutputFile(String filePath) {
        final String START_PROGRAM_LABEL = "#START_PROGRAM_LABEL";
        final String END_PROGRAM_LABEL = "#END_PROGRAM_LABEL";
        FileInputStream fileInputStream;
        FileOutputStream fileOutputStream;
        try {
            fileInputStream = new FileInputStream(filePath);
            fileOutputStream = new FileOutputStream(filePath.replace("run", "exec"));

            CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
            decoder.onMalformedInput(CodingErrorAction.IGNORE);

            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, decoder);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);

            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

            String line = bufferedReader.readLine();
            boolean writeFlag = false;
            while (line != null) {
                if (line.contains(START_PROGRAM_LABEL)) {
                    writeFlag = true;
                    line = bufferedReader.readLine();
                    continue;
                }
                if (line.contains(END_PROGRAM_LABEL)) {
                    writeFlag = false;
                    line = bufferedReader.readLine();
                    continue;
                }
                if (writeFlag)
                    bufferedWriter.write(line + "\n");
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
            bufferedWriter.close();
        } catch (IOException ex) {
            LOGGER.error("Error while create program output log. Message: " + ex.getMessage(), ex);
        }
    }

    private DataSource initStorageDataSource() {
        DataSourceBuilder<?> builder = DataSourceBuilder.create();
        builder.type(PGSimpleDataSource.class);
        builder.driverClassName("org.postgresql.Driver");
        builder.url(agentParamRepository.getByName("DB_URL").getValue());
        builder.username(agentParamRepository.getByName("DB_USER").getValue());
        builder.password(agentParamRepository.getByName("DB_PASSWORD").getValue());
        return builder.build();
    }

    private DataSource initStageInfoDataSource() {
        DataSourceBuilder<?> builder = DataSourceBuilder.create();
        builder.type(PGSimpleDataSource.class);
        builder.driverClassName("org.postgresql.Driver");
        builder.url(agentParamRepository.getByName("DB_URL").getValue());
        builder.username(agentParamRepository.getByName("DB_LOG_USER").getValue());
        builder.password(agentParamRepository.getByName("DB_LOG_PASSWORD").getValue());
        return builder.build();
    }

    private boolean debugMode() {
        AgentParam debugModeParam = agentParamRepository.getByName("AGENT_DEBUG_MODE");
        if (debugModeParam == null) {
            LOGGER.error("Agent parameter with name: AGENT_DEBUG_MODE not found");
            return false;
        }
        String debugMode = debugModeParam.getValue();
        debugMode = debugMode.toLowerCase();
        if (debugMode.equals("1") || debugMode.equals("true")) {
            return true;
        }
        return false;
    }
}
