package g3.rm.resourcemanager.entities;

import jakarta.persistence.*;

@Entity
public class ManagerParam {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "manager_param_gen")
    @SequenceGenerator(name = "manager_param_gen", sequenceName = "MANAGER_PARAM_SEQ", allocationSize = 1)
    private Long id;
    private String paramName;
    private String paramValue;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String name) {
        this.paramName = name;
    }

    public String getParamValue() {
        return paramValue;
    }

    public void setParamValue(String value) {
        this.paramValue = value;
    }
}