package g3.rm.resourcemanager.consumers;

import g3.rm.resourcemanager.message.KafkaMessage;
import g3.rm.resourcemanager.message.MessageContent;
import g3.rm.resourcemanager.repositories.state.RouteStatusRepository;
import g3.rm.resourcemanager.repositories.state.TopicMessageRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MessageConsumerService {

    private final TopicMessageRepository topicMessageRepository;
    private final RouteStatusRepository routeStatusRepository;
    private final String TOPIC_NAME = "rm-topic";
    private final Logger LOGGER = LogManager.getLogger(MessageConsumerService.class);

    public MessageConsumerService(TopicMessageRepository topicMessageRepository, RouteStatusRepository routeStatusRepository) {
        this.topicMessageRepository = topicMessageRepository;
        this.routeStatusRepository = routeStatusRepository;
    }

    @KafkaListener(topics = TOPIC_NAME, groupId = "rm")
    public void receiveMessage(UUID message_uuid, Acknowledgment acknowledgment) {
        KafkaMessage message = topicMessageRepository.getMessage(message_uuid);
        if (message.getIs_received()) {
            LOGGER.info("Message received earlier. Message: " + message);
            acknowledgment.acknowledge();
            return;
        }
        if (routeStatusRepository.isRouteNotActive(message.getRoute_id())) {
            LOGGER.info("Route for this message is not active. Message: " + message);
            acknowledgment.acknowledge();
            return;
        }

        LOGGER.info("Message received: " +  message);
        MessageContent content = KafkaMessage.getContentObject(message);
        LOGGER.info("Message content: " + content);

        try {
            // Test exception
            if (message.getRoute_id() == 5) {
                throw new RuntimeException("Test exception while processing route: " + message.getRoute_id());
            }
        } catch (RuntimeException ex) {
            LOGGER.error("Runtime exception while processing message: " + message, ex);
            routeStatusRepository.setRouteStatus(message.getRoute_id(), -1);
            return;
        }

        topicMessageRepository.commitReceiveMessage(message_uuid);
        acknowledgment.acknowledge();
    }
}
