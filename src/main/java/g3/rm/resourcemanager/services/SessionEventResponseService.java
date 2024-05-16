package g3.rm.resourcemanager.services;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SessionEventResponseService {
    public void setCheckInitEvent(long taskId, long sessionId) {

    }

    public void setCheckDoneEvent(long taskId, long sessionId, int code) {

    }

    public void setDeployInitEvent(long taskId, long sessionId) {

    }

    public void setDeployDoneEvent(long taskId, long sessionId, int code) {

    }

    public void sendRunInitEvent(long taskId, long sessionId) {

    }

    public void sendRunDoneEvent(long taskId, long sessionId, List<String> deviceNameList, int exitCode, int programCode, String boomerangCode) {

    }

    public void setStopInitEvent(long taskId, long sessionId) {

    }

    public void setStopDoneEvent(long taskId, long sessionId, int code) {

    }

    public void setCollectInitEvent(long taskId, long sessionId) {

    }

    public void setCollectDoneEvent(long taskId, long sessionId, int code) {

    }

    public void setProgressInfo(String progressInfo) {

    }

    public void sendSessionEnd(long taskId, long sessionId, int status) {

    }

    public void sendSessionStop(long taskId, long sessionId, int status) {

    }
}

