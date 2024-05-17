package g3.rm.resourcemanager.dtos;

public class DecisionItem {
    private Long taskId;
    private String deviceName;

    public DecisionItem() {
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    @Override
    public String toString() {
        return "DecisionItem{" +
                "taskId=" + taskId +
                ", deviceName='" + deviceName + '\'' +
                '}';
    }
}
