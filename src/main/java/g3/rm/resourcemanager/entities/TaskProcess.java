package g3.rm.resourcemanager.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class TaskProcess {
    @Id
    private Long stageId;
    private String operation;
    private Long entityId;

    public TaskProcess() {
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }
}
