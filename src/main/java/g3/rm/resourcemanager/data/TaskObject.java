package g3.rm.resourcemanager.data;

import java.util.ArrayList;
import java.util.List;

public class TaskObject {
    private long eventId;
    private long taskId;
    private long sessionId;
    private int programId;
    private List<String> deviceNameList;
    private String bucketName;
    private String objectName;
    private String sessionStatus;

    public TaskObject() {
        this.deviceNameList = new ArrayList<>(4);
        this.sessionStatus = "";
    }

    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public int getProgramId() {
        return programId;
    }

    public void setProgramId(int programId) {
        this.programId = programId;
    }

    public List<String> getDeviceNameList() {
        return deviceNameList;
    }

    public void setDeviceNameList(List<String> deviceNameList) {
        this.deviceNameList = deviceNameList;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        if (this.sessionStatus.isEmpty()) {
            this.sessionStatus = sessionStatus;
        }
    }

    @Override
    public String toString() {
        return "TaskObject{" +
                "eventId=" + eventId +
                ", taskId=" + taskId +
                ", sessionId=" + sessionId +
                ", programId=" + programId +
                ", deviceNameList=" + deviceNameList +
                ", bucketName='" + bucketName + '\'' +
                ", objectName='" + objectName + '\'' +
                ", sessionStatus='" + sessionStatus + '\'' +
                '}';
    }
}
