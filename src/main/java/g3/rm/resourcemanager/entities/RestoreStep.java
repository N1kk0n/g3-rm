package g3.rm.resourcemanager.entities;

import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
public class RestoreStep {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "restore_step_gen")
    @SequenceGenerator(name = "restore_step_gen", sequenceName = "RESTORE_STEP_SEQ", allocationSize = 1)
    private Long id;
    private Timestamp time;
    private String query;
    private String params;

    public RestoreStep() {
        this.time = new Timestamp(System.currentTimeMillis());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String taskId) {
        this.query = taskId;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String action) {
        this.params = action;
    }
}
