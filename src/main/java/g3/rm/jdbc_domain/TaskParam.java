package g3.rm.jdbc_domain;

public class TaskParam {
    private Integer programId;
    private String paramName;
    private String paramValue;

    public TaskParam() {
    }

    public Integer getProgramId() {
        return programId;
    }

    public void setProgramId(Integer programId) {
        this.programId = programId;
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
        return "TaskParam{" +
                "programId=" + programId +
                ", paramName='" + paramName + '\'' +
                ", paramValue='" + paramValue + '\'' +
                '}';
    }
}
