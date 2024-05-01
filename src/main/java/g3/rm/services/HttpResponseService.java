package g3.rm.services;

import g3.rm.jpa_domain.AgentParam;
import g3.rm.jpa_domain.RestoreStep;
import g3.rm.repositories.AgentParamRepository;
import g3.rm.repositories.RestoreStepRepository;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class HttpResponseService {
    @Autowired
    private AgentParamRepository agentParamRepository;
    @Autowired
    private RestoreStepRepository restoreStepRepository;
    @Autowired
    private MailSenderService mailSenderService;

    private final Logger LOGGER = LogManager.getLogger("HttpResponseService");

    public void sendCheckDeviceResponse(long eventId, String deviceName, int code) {
        if (code != 0) {
            mailSenderService.sendDeviceBrokenMail(deviceName, code);
        }

        AgentParam urlParam = agentParamRepository.getByName("CHECK_DEVICE_URL");
        if (urlParam == null) {
            LOGGER.error("Agent parameter CHECK_DEVICE_URL not found");
            return;
        }
        String checkResponseUrl = urlParam.getValue();
        checkResponseUrl = checkResponseUrl.replaceAll("EVENT_ID", String.valueOf(eventId));
        checkResponseUrl = checkResponseUrl.replaceAll("DEVICE_NAME", deviceName);
        checkResponseUrl = checkResponseUrl.replaceAll("CODE", String.valueOf(code));

        if (debugMode()) {
            LOGGER.debug("Call RManager [DEBUG MODE] (No response): " + checkResponseUrl);
            return;
        }
        LOGGER.info("Call RManager: " + checkResponseUrl);
        JsonObject object = sendRestRequest(checkResponseUrl, HttpMethod.GET.name(), "");
        if (!object.containsKey("code") || object.getInt("code") != 200) {
            LOGGER.error("Error of call RManager: " + checkResponseUrl);
        }
    }

    public void sendCheckResponse(long eventId, long taskId, long sessionId, int code) {
        AgentParam urlParam = agentParamRepository.getByName("CHECK_TASK_URL");
        if (urlParam == null) {
            LOGGER.error("Agent parameter CHECK_TASK_URL not found");
            return;
        }
        String checkResponseUrl = urlParam.getValue();
        checkResponseUrl = checkResponseUrl.replaceAll("EVENT_ID", String.valueOf(eventId));
        checkResponseUrl = checkResponseUrl.replaceAll("TASK_ID", String.valueOf(taskId));
        checkResponseUrl = checkResponseUrl.replaceAll("SESSION_ID", String.valueOf(sessionId));
        checkResponseUrl = checkResponseUrl.replaceAll("CODE", String.valueOf(code));

        if (debugMode()) {
            LOGGER.debug("Call RManager [DEBUG MODE] (No response): " + checkResponseUrl);
            return;
        }
        LOGGER.info("Call RManager: " + checkResponseUrl);
        JsonObject object = sendRestRequest(checkResponseUrl, HttpMethod.GET.name(), "");
        if (!object.containsKey("code") || object.getInt("code") != 200) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(checkResponseUrl);
            restoreStepRepository.save(restoreStep);
        }
    }

    public void sendDeployInitResponse(long taskId, long sessionId) {
        AgentParam urlParam = agentParamRepository.getByName("DEPLOY_INIT_URL");
        if (urlParam == null) {
            LOGGER.error("Agent parameter DEPLOY_INIT_URL not found");
            return;
        }
        String deployResponseUrl = urlParam.getValue();
        deployResponseUrl = deployResponseUrl.replaceAll("TASK_ID", String.valueOf(taskId));
        deployResponseUrl = deployResponseUrl.replaceAll("SESSION_ID", String.valueOf(sessionId));

        if (debugMode()) {
            LOGGER.debug("Call RManager [DEBUG MODE] (No response): " + deployResponseUrl);
            return;
        }
        LOGGER.info("Call RManager: " + deployResponseUrl);
        if (checkRestoreStep(String.valueOf(taskId))) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(deployResponseUrl);
            restoreStepRepository.save(restoreStep);
            return;
        }
        JsonObject object = sendRestRequest(deployResponseUrl, HttpMethod.GET.name(), "");
        if (!object.containsKey("code") || object.getInt("code") != 200) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(deployResponseUrl);
            restoreStepRepository.save(restoreStep);
        }
    }

    public void sendDeployDoneResponse(long taskId, long sessionId, int code) {
        AgentParam urlParam = agentParamRepository.getByName("DEPLOY_DONE_URL");
        if (urlParam == null) {
            LOGGER.error("Agent parameter DEPLOY_DONE_URL not found");
            return;
        }
        String deployResponseUrl = urlParam.getValue();
        deployResponseUrl = deployResponseUrl.replaceAll("TASK_ID", String.valueOf(taskId));
        deployResponseUrl = deployResponseUrl.replaceAll("SESSION_ID", String.valueOf(sessionId));
        deployResponseUrl = deployResponseUrl.replaceAll("CODE", String.valueOf(code));
        if (debugMode()) {
            LOGGER.debug("Call RManager [DEBUG MODE] (No response): " + deployResponseUrl);
            return;
        }

        LOGGER.info("Call RManager: " + deployResponseUrl);
        if (checkRestoreStep(String.valueOf(taskId))) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(deployResponseUrl);
            restoreStepRepository.save(restoreStep);
            return;
        }
        JsonObject object = sendRestRequest(deployResponseUrl, HttpMethod.GET.name(), "");
        if (!object.containsKey("code") || object.getInt("code") != 200) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(deployResponseUrl);
            restoreStepRepository.save(restoreStep);
        }
    }

    public void sendRunInitResponse(long taskId, long sessionId) {
        AgentParam urlParam = agentParamRepository.getByName("RUN_INIT_URL");
        if (urlParam == null) {
            LOGGER.error("Agent parameter RUN_INIT_URL not found");
            return;
        }
        String runResponseUrl = urlParam.getValue();
        runResponseUrl = runResponseUrl.replaceAll("TASK_ID", String.valueOf(taskId));
        runResponseUrl = runResponseUrl.replaceAll("SESSION_ID", String.valueOf(sessionId));
        if (debugMode()) {
            LOGGER.debug("Call RManager [DEBUG MODE] (No response): " + runResponseUrl);
            return;
        }

        LOGGER.info("Call RManager: " + runResponseUrl);
        if (checkRestoreStep(String.valueOf(taskId))) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(runResponseUrl);
            restoreStepRepository.save(restoreStep);
            return;
        }
        JsonObject object = sendRestRequest(runResponseUrl, HttpMethod.GET.name(), "");
        if (!object.containsKey("code") || object.getInt("code") != 200) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(runResponseUrl);
            restoreStepRepository.save(restoreStep);
        }
    }

    public void sendRunDoneResponse(long taskId,
                                    long sessionId,
                                    List<String> deviceList,
                                    int exitCode,
                                    int programCode,
                                    String boomerangCode) {
        AgentParam urlParam = agentParamRepository.getByName("RUN_DONE_URL");
        if (urlParam == null) {
            LOGGER.error("Agent parameter RUN_DONE_URL not found");
            return;
        }
        String runResponseUrl = urlParam.getValue();
        runResponseUrl = runResponseUrl.replaceAll("TASK_ID", String.valueOf(taskId));
        runResponseUrl = runResponseUrl.replaceAll("SESSION_ID", String.valueOf(sessionId));
        String deviceUrlList = "";
        for (int i = 0; i < deviceList.size(); i++) {
            String deviceName = deviceList.get(i);
            if (i == deviceList.size() - 1) {
                deviceUrlList += "device_list=" + deviceName;
            } else {
                deviceUrlList += "device_list=" + deviceName + "&";
            }
        }
        runResponseUrl = runResponseUrl.replaceAll("DEVICE_LIST", deviceUrlList);
        runResponseUrl = runResponseUrl.replaceAll("CODE", String.valueOf(exitCode));
        runResponseUrl = runResponseUrl.replaceAll("PROGRAM", String.valueOf(programCode));
        runResponseUrl = runResponseUrl.replaceAll("BOOMERANG", boomerangCode);
        if (debugMode()) {
            LOGGER.debug("Call RManager [DEBUG MODE] (No response): " + runResponseUrl);
            return;
        }
        
        LOGGER.info("Call RManager: " + runResponseUrl);
        if (checkRestoreStep(String.valueOf(taskId))) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(runResponseUrl);
            restoreStepRepository.save(restoreStep);
            return;
        }
        JsonObject object = sendRestRequest(runResponseUrl, HttpMethod.GET.name(), "");
        if (!object.containsKey("code") || object.getInt("code") != 200) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(runResponseUrl);
            restoreStepRepository.save(restoreStep);
        }
    }

    public void sendStopInitResponse(long taskId, long sessionId) {
        AgentParam urlParam = agentParamRepository.getByName("STOP_INIT_URL");
        if (urlParam == null) {
            LOGGER.error("Agent parameter STOP_INIT_URL not found");
            return;
        }
        String stopResponseUrl = urlParam.getValue();
        stopResponseUrl = stopResponseUrl.replaceAll("TASK_ID", String.valueOf(taskId));
        stopResponseUrl = stopResponseUrl.replaceAll("SESSION_ID", String.valueOf(sessionId));
        if (debugMode()) {
            LOGGER.debug("Call RManager [DEBUG MODE] (No response): " + stopResponseUrl);
            return;
        }
        
        LOGGER.info("Call RManager: " + stopResponseUrl);
        if (checkRestoreStep(String.valueOf(taskId))) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(stopResponseUrl);
            restoreStepRepository.save(restoreStep);
            return;
        }
        JsonObject object = sendRestRequest(stopResponseUrl, HttpMethod.GET.name(), "");
        if (!object.containsKey("code") || object.getInt("code") != 200) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(stopResponseUrl);
            restoreStepRepository.save(restoreStep);
        }
    }

    public void sendStopDoneResponse(long taskId, long sessionId, int code) {
        AgentParam urlParam = agentParamRepository.getByName("STOP_DONE_URL");
        if (urlParam == null) {
            LOGGER.error("Agent parameter STOP_DONE_URL not found");
            return;
        }
        String stopResponseUrl = urlParam.getValue();
        stopResponseUrl = stopResponseUrl.replaceAll("TASK_ID", String.valueOf(taskId));
        stopResponseUrl = stopResponseUrl.replaceAll("SESSION_ID", String.valueOf(sessionId));
        stopResponseUrl = stopResponseUrl.replaceAll("CODE", String.valueOf(code));
        if (debugMode()) {
            LOGGER.debug("Call RManager [DEBUG MODE] (No response): " + stopResponseUrl);
            return;
        }

        LOGGER.info("Call RManager: " + stopResponseUrl);
        if (checkRestoreStep(String.valueOf(taskId))) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(stopResponseUrl);
            restoreStepRepository.save(restoreStep);
            return;
        }
        JsonObject object = sendRestRequest(stopResponseUrl, HttpMethod.GET.name(), "");
        if (!object.containsKey("code") || object.getInt("code") != 200) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(stopResponseUrl);
            restoreStepRepository.save(restoreStep);
        }
    }

    public void sendCollectInitResponse(long taskId, long sessionId) {
        AgentParam urlParam = agentParamRepository.getByName("COLLECT_INIT_URL");
        if (urlParam == null) {
            LOGGER.error("Agent parameter COLLECT_INIT_URL not found");
            return;
        }
        String collectResponseUrl = urlParam.getValue();
        collectResponseUrl = collectResponseUrl.replaceAll("TASK_ID", String.valueOf(taskId));
        collectResponseUrl = collectResponseUrl.replaceAll("SESSION_ID", String.valueOf(sessionId));
        if (debugMode()) {
            LOGGER.debug("Call RManager [DEBUG MODE] (No response): " + collectResponseUrl);
            return;
        }

        LOGGER.info("Call RManager: " + collectResponseUrl);
        if (checkRestoreStep(String.valueOf(taskId))) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(collectResponseUrl);
            restoreStepRepository.save(restoreStep);
            return;
        }
        JsonObject object = sendRestRequest(collectResponseUrl, HttpMethod.GET.name(), "");
        if (!object.containsKey("code") || object.getInt("code") != 200) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(collectResponseUrl);
            restoreStepRepository.save(restoreStep);
        }
    }

    public void sendCollectDoneResponse(long taskId, long sessionId, int code) {
        AgentParam urlParam = agentParamRepository.getByName("COLLECT_DONE_URL");
        if (urlParam == null) {
            LOGGER.error("Agent parameter COLLECT_DONE_URL not found");
            return;
        }
        String collectResponseUrl = urlParam.getValue();
        collectResponseUrl = collectResponseUrl.replaceAll("TASK_ID", String.valueOf(taskId));
        collectResponseUrl = collectResponseUrl.replaceAll("SESSION_ID", String.valueOf(sessionId));
        collectResponseUrl = collectResponseUrl.replaceAll("CODE", String.valueOf(code));
        if (debugMode()) {
            LOGGER.debug("Call RManager [DEBUG MODE] (No response): " + collectResponseUrl);
            return;
        }

        LOGGER.info("Call RManager: " + collectResponseUrl);
        if (checkRestoreStep(String.valueOf(taskId))) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(collectResponseUrl);
            restoreStepRepository.save(restoreStep);
            return;
        }
        JsonObject object = sendRestRequest(collectResponseUrl, HttpMethod.GET.name(), "");
        if (!object.containsKey("code") || object.getInt("code") != 200) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(collectResponseUrl);
            restoreStepRepository.save(restoreStep);
        }
    }

    public void sendProgressInfoResponse(String progressInfo) {
        AgentParam urlParam = agentParamRepository.getByName("PROGRESS_INFO_URL");
        if (urlParam == null) {
            LOGGER.error("Agent parameter PROGRESS_INFO_URL not found");
            return;
        }
        String progressInfoUrl = urlParam.getValue();
        if (debugMode()) {
            LOGGER.debug("Call RManager [DEBUG MODE] (No response): " + progressInfoUrl + " " + progressInfo);
            return;
        }

        LOGGER.info("Call RManager: " + progressInfoUrl);
        JsonObject object = sendRestRequest(progressInfoUrl, HttpMethod.POST.name(), progressInfo);
        if (!object.containsKey("code") || object.getInt("code") != 200) {
            LOGGER.error("Error of call RManager: " + progressInfoUrl + " Data: " + progressInfo);
        }
    }

    public void sendFinalProgressInfoResponse(String progressInfo) {
        AgentParam urlParam = agentParamRepository.getByName("FINAL_PROGRESS_INFO_URL");
        if (urlParam == null) {
            LOGGER.error("Agent parameter FINAL_PROGRESS_INFO_URL not found");
            return;
        }
        String finalProgressInfoUrl = urlParam.getValue();
        if (debugMode()) {
            LOGGER.debug("Call RManager [DEBUG MODE] (No response): " + finalProgressInfoUrl + " " + progressInfo);
            return;
        }

        LOGGER.info("Call RManager: " + finalProgressInfoUrl);
        JsonObject object = sendRestRequest(finalProgressInfoUrl, HttpMethod.POST.name(), progressInfo);
        if (!object.containsKey("code") || object.getInt("code") != 200) {
            LOGGER.error("Error of call RManager: " + finalProgressInfoUrl + " Data: " + progressInfo);
        }
    }

    public void sendSessionEnd(long eventId, long taskId, long sessionId, int code) {
        AgentParam urlParam = agentParamRepository.getByName("SESSION_END_URL");
        if (urlParam == null) {
            LOGGER.error("Agent parameter SESSION_END_URL not found");
            return;
        }
        String sessionEndUrl = urlParam.getValue();
        sessionEndUrl = sessionEndUrl.replaceAll("EVENT_ID", String.valueOf(eventId));
        sessionEndUrl = sessionEndUrl.replaceAll("TASK_ID", String.valueOf(taskId));
        sessionEndUrl = sessionEndUrl.replaceAll("SESSION_ID", String.valueOf(sessionId));
        sessionEndUrl = sessionEndUrl.replaceAll("CODE", String.valueOf(code));
        if (debugMode()) {
            LOGGER.debug("Call RManager [DEBUG MODE] (No response): " + sessionEndUrl);
            return;
        }

        LOGGER.info("Call RManager: " + sessionEndUrl);
        if (checkRestoreStep(String.valueOf(taskId))) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(sessionEndUrl);
            restoreStepRepository.save(restoreStep);
            return;
        }
        JsonObject object = sendRestRequest(sessionEndUrl, HttpMethod.GET.name(), "");
        if (!object.containsKey("code") || object.getInt("code") != 200) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(sessionEndUrl);
            restoreStepRepository.save(restoreStep);
        }
    }

    public void sendSessionStop(long taskId, long sessionId, int code) {
        AgentParam urlParam = agentParamRepository.getByName("SESSION_STOP_URL");
        if (urlParam == null) {
            LOGGER.error("Agent parameter SESSION_STOP_URL not found");
            return;
        }
        String sessionStopUrl = urlParam.getValue();
        sessionStopUrl = sessionStopUrl.replaceAll("TASK_ID", String.valueOf(taskId));
        sessionStopUrl = sessionStopUrl.replaceAll("SESSION_ID", String.valueOf(sessionId));
        sessionStopUrl = sessionStopUrl.replaceAll("CODE", String.valueOf(code));
        if (debugMode()) {
            LOGGER.debug("Call RManager [DEBUG MODE] (No response): " + sessionStopUrl);
            return;
        }

        LOGGER.info("Call RManager: " + sessionStopUrl);
        if (checkRestoreStep(String.valueOf(taskId))) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(sessionStopUrl);
            restoreStepRepository.save(restoreStep);
            return;
        }
        JsonObject object = sendRestRequest(sessionStopUrl, HttpMethod.GET.name(), "");
        if (!object.containsKey("code") || object.getInt("code") != 200) {
            RestoreStep restoreStep = new RestoreStep();
            restoreStep.setTaskId(String.valueOf(taskId));
            restoreStep.setUrl(sessionStopUrl);
            restoreStepRepository.save(restoreStep);
        }
    }

    public JsonObject sendRestRequest(String serviceUrl, String method, String data) {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        URL url;
        HttpURLConnection con = null;
        BufferedReader in = null;
        try {
            url = new URL(serviceUrl);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON.getType());
            con.setRequestProperty("Accept", MediaType.APPLICATION_JSON.getType());
            con.setRequestMethod(method);
            if (method.equals(HttpMethod.POST.name())) {
                con.setDoOutput(true);
                OutputStream os = con.getOutputStream();
                byte[] input = data.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            if ((con.getResponseCode() != 400) && con.getResponseCode() != 404) {
                in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {
                in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            builder.add("output", content.toString());
            in.close();
            con.disconnect();
            builder.add("code", con.getResponseCode());
        } catch (IOException ex) {
            LOGGER.error("Exception while trying send REST request: " + ex.getMessage(), ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
            if (con != null) {
                con.disconnect();
            }
        }
        return builder.build();
    }

    private boolean checkRestoreStep(String taskId) {
        return restoreStepRepository.existsByTaskId(taskId);
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
