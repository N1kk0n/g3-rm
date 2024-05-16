package g3.rm.resourcemanager.entities;

import jakarta.persistence.*;

@Entity
public class ProgramParam {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "program_param_gen")
    @SequenceGenerator(name = "program_param_gen", sequenceName = "PROGRAM_PARAM_SEQ", allocationSize = 1)
    private Long id;
    private Integer programId;
    private String paramName;
    private String paramValue;

    public ProgramParam() {
    }

    public ProgramParam(Integer programId, String paramName, String paramValue) {
        this.programId = programId;
        this.paramName = paramName;
        this.paramValue = paramValue;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
