package g3.rm.resourcemanager.routing.dtos.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class Content {
    private Long route_id;
    private Integer graph_id;
    private String operation;
    private Long task_id;
    private Long session_id;
    private Integer program_id;
    private List<String> device_name_list;
    private List<Operation> log;

    public Content(Long route_id, Integer graph_id, String operation) {
        this.route_id = route_id;
        this.graph_id = graph_id;
        this.operation = operation;
        this.device_name_list = new ArrayList<>();
        this.log = new ArrayList<>();
    }

    public static String json(Content content) {
        Logger logger = LogManager.getLogger(Content.class);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            logger.error("Error while create JSON from object: " + content, e);
            return "";
        }
    }
}
