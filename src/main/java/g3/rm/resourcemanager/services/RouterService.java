package g3.rm.resourcemanager.services;

import g3.rm.resourcemanager.dtos.NextRouteVertex;
import g3.rm.resourcemanager.message.KafkaMessage;
import g3.rm.resourcemanager.message.MessageContent;
import g3.rm.resourcemanager.repositories.inner.RouterRepository;
import g3.rm.resourcemanager.repositories.state.TopicMessageRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class RouterService {

    private final RouterRepository routerRepository;
    private final TopicMessageRepository topicMessageRepository;
    private final Logger LOGGER = LogManager.getLogger(RouterService.class);

    public RouterService(RouterRepository routerRepository, TopicMessageRepository topicMessageRepository) {
        this.routerRepository = routerRepository;
        this.topicMessageRepository = topicMessageRepository;
    }

    public KafkaMessage onRoute(KafkaMessage kafkaMessage) {
        MessageContent content = KafkaMessage.getContentObject(kafkaMessage);
        String operation = content.getOperation();
//        return switch (operation) {
//            case "TEST" -> {
//                int code = 2;
//                NextRouteVertex nextRouteVertex = routerRepository.route();
//                yield new KafkaMessage();
//            }
//            case "END" -> {
//                topicMessageRepository.deleteRoute(kafkaMessage.getRoute_id());
//                yield null;
//            }
//        };
        return null;
    }

    public KafkaMessage createRoute() {
        return new KafkaMessage();
    }
}
