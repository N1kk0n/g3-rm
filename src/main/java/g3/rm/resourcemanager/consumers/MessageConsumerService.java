package g3.rm.resourcemanager.consumers;

import g3.rm.resourcemanager.message.KafkaMessage;
import g3.rm.resourcemanager.repositories.state.StateRouteRepository;
import g3.rm.resourcemanager.repositories.state.TopicMessageRepository;
import g3.rm.resourcemanager.services.RouterService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MessageConsumerService {

    private final TopicMessageRepository topicMessageRepository;
    private final StateRouteRepository stateRouteRepository;
    private final RouterService routerService;
    private final String TOPIC_NAME = "rm-topic";
    private final Logger LOGGER = LogManager.getLogger(MessageConsumerService.class);

    public MessageConsumerService(TopicMessageRepository topicMessageRepository,
                                  StateRouteRepository stateRouteRepository,
                                  RouterService routerService) {
        this.topicMessageRepository = topicMessageRepository;
        this.stateRouteRepository = stateRouteRepository;
        this.routerService = routerService;
    }

    @KafkaListener(topics = TOPIC_NAME, groupId = "rm")
    public void receiveMessage(UUID message_uuid, Acknowledgment acknowledgment) {
        KafkaMessage message = topicMessageRepository.getMessage(message_uuid);
        if (message.getIs_received()) {
            LOGGER.info("Message received earlier. Message: " + message);
            acknowledgment.acknowledge();
            return;
        }
        if (stateRouteRepository.isRouteNotActive(message.getRoute_id())) {
            LOGGER.info("Route for this message is not active. Message: " + message);
            acknowledgment.acknowledge();
            return;
        }
        LOGGER.info("Message received: " +  message);

        try {
            routerService.onRoute(message);
        } catch (RuntimeException ex) {
            LOGGER.error("Runtime exception while processing message: " + message, ex);
            stateRouteRepository.setRouteStatus(message.getRoute_id(), -1);
            return;
        }

        topicMessageRepository.commitReceiveMessage(message_uuid);
        acknowledgment.acknowledge();
    }
}
