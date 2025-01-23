package g3.rm.resourcemanager.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class MessageContent {
    private Long route_id;
    private Long graph_id;
    private String operation;
    private Long task_id;
    private Long session_id;
    private List<String> device_name_list = new LinkedList<>();
    private List<Operation> log = new LinkedList<>();

    public static String json(MessageContent content) {
        Logger logger = LogManager.getLogger(MessageContent.class);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            logger.error("Error while create JSON from object: " + content, e);
            return "";
        }
    }
}
