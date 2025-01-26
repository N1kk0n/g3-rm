package g3.rm.resourcemanager.services;

import g3.rm.resourcemanager.dtos.RouteVertex;
import g3.rm.resourcemanager.message.KafkaMessage;
import g3.rm.resourcemanager.message.MessageContent;
import g3.rm.resourcemanager.message.Operation;
import g3.rm.resourcemanager.producers.MessageProducerService;
import g3.rm.resourcemanager.repositories.inner.InnerRouteRepository;
import g3.rm.resourcemanager.repositories.state.StateRouteRepository;
import g3.rm.resourcemanager.repositories.state.TopicMessageRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class RouterService {

    private final InnerRouteRepository innerRouteRepository;
    private final StateRouteRepository stateRouteRepository;
    private final MessageProducerService messageProducerService;
    private final TopicMessageRepository topicMessageRepository;
    private final String SELF_NAME = "rm";

    public RouterService(InnerRouteRepository innerRouteRepository,
                         StateRouteRepository stateRouteRepository,
                         MessageProducerService messageProducerService,
                         TopicMessageRepository topicMessageRepository) {
        this.innerRouteRepository = innerRouteRepository;
        this.stateRouteRepository = stateRouteRepository;
        this.messageProducerService = messageProducerService;
        this.topicMessageRepository = topicMessageRepository;
    }

    public void createRoute(int graphId) {
        long routeId = stateRouteRepository.createRoute(graphId);
        RouteVertex firstVertex = innerRouteRepository.getFirstVertex(graphId);

        MessageContent content = new MessageContent(routeId, graphId,firstVertex.getOperation());
        content.setLog(Collections.emptyList());

        KafkaMessage kafkaMessage = new KafkaMessage();
        kafkaMessage.setRoute_id(routeId);
        kafkaMessage.setProducer(SELF_NAME);
        kafkaMessage.setConsumer(firstVertex.getConsumer());
        kafkaMessage.setIs_received(false);
        kafkaMessage.setContent(MessageContent.json(content));

        messageProducerService.sendMessage(firstVertex.getTopic(), kafkaMessage);
    }

    public void continueRoute(Long routeId, int graphId, String currOperation, int currOperationResult, String nextOperation, String nextComponent, String nextTopic) {
        MessageContent content = new MessageContent(routeId, graphId, nextOperation);

        List<Operation> logList = content.getLog();
        logList.add(new Operation(routeId, currOperation, SELF_NAME, currOperationResult));
        content.setLog(logList);

        KafkaMessage kafkaMessage = new KafkaMessage();
        kafkaMessage.setRoute_id(routeId);
        kafkaMessage.setProducer(SELF_NAME);
        kafkaMessage.setConsumer(nextComponent);
        kafkaMessage.setIs_received(false);
        kafkaMessage.setContent(MessageContent.json(content));

        messageProducerService.sendMessage(nextTopic, kafkaMessage);
    }

    public void onRoute(KafkaMessage message) {
        MessageContent content = KafkaMessage.getContentObject(message);
        long routeId = content.getRoute_id();
        int graphId = content.getGraph_id();
        String operation = content.getOperation();

        switch (operation) {
            case "TEST" -> {
                int code = 2;
                RouteVertex nextRouteVertex = innerRouteRepository.route(content.getGraph_id(), operation, code);
                if (!Objects.equals(nextRouteVertex.getGraph_id(), graphId)) {
                    topicMessageRepository.deleteRouteMessages(routeId);
                    createRoute(nextRouteVertex.getGraph_id());
                } else {
                    continueRoute(routeId, graphId, "TEST", code, nextRouteVertex.getOperation(), nextRouteVertex.getConsumer(), nextRouteVertex.getTopic());
                }
            }
            case "END" -> {
                topicMessageRepository.deleteRouteMessages(routeId);
                stateRouteRepository.deleteRoute(routeId);
            }
        }
    }
}
