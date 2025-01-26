package g3.rm.resourcemanager.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class KafkaMessage {
    private UUID unique_id = UUID.randomUUID();
    private Long route_id;
    private String producer;
    private String consumer;
    private Boolean is_received;
    private String content;

    public static MessageContent getContentObject(KafkaMessage message) {
        Logger logger = LogManager.getLogger(KafkaMessage.class);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(message.getContent(), MessageContent.class);
        } catch (JsonProcessingException e) {
            logger.error("Error while create object from JSON: " + message, e);
            return new MessageContent(null, null, null);
        }
    }
}
