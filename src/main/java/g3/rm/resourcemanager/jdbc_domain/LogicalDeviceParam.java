package g3.rm.resourcemanager.jdbc_domain;

public class LogicalDeviceParam {
    private Integer deviceId;
    private String deviceName;
    private String paramName;
    private String paramValue;

    public LogicalDeviceParam() {
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public String getParamValue() {
        return paramValue;
    }

    public void setParamValue(String paramValue) {
        this.paramValue = paramValue;
    }

    @Override
    public String toString() {
        return "LogicalDeviceParam{" +
                "deviceId=" + deviceId +
                ", deviceName='" + deviceName + '\'' +
                ", paramName='" + paramName + '\'' +
                ", paramValue='" + paramValue + '\'' +
                '}';
    }
}
