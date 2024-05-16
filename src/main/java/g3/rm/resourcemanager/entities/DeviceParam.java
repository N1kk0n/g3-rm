package g3.rm.resourcemanager.entities;


import jakarta.persistence.*;

@Entity
public class DeviceParam {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "device_param_gen")
    @SequenceGenerator(name = "device_param_gen", sequenceName = "DEVICE_PARAM_SEQ", allocationSize = 1)
    private Long id;
    private Integer deviceId;
    private String deviceName;
    private String paramName;
    private String paramValue;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public void setDeviceName(String lvuName) {
        this.deviceName = lvuName;
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
}
