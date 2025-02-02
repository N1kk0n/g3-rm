package g3.rm.resourcemanager.services;

import g3.rm.resourcemanager.actions.Action;
import g3.rm.resourcemanager.actions.Test;
import g3.rm.resourcemanager.dtos.RouteVertex;
import g3.rm.resourcemanager.dtos.kafka.Message;
import g3.rm.resourcemanager.dtos.kafka.Content;
import g3.rm.resourcemanager.dtos.kafka.Operation;
import g3.rm.resourcemanager.services.kafka.MessageProducerService;
import g3.rm.resourcemanager.repositories.inner.InnerRouteRepository;
import g3.rm.resourcemanager.repositories.state.StateRouteRepository;
import g3.rm.resourcemanager.repositories.state.TopicMessageRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
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

    public void createRoute(String graphName, Content params) {
        int graphId = innerRouteRepository.getGraphId(graphName);
        createRouteById(graphId, params);
    }

    private void createRouteById(int graphId, Content params) {
        long routeId = stateRouteRepository.createRoute(graphId);
        RouteVertex firstVertex = innerRouteRepository.getFirstVertex(graphId);

        Content content = new Content(routeId, graphId,firstVertex.getOperation());
        content.setLog(Collections.emptyList());
        content.setTask_id(params.getTask_id());
        content.setSession_id(params.getSession_id());
        content.setProgram_id(params.getProgram_id());
        content.setDevice_name_list(params.getDevice_name_list());

        Message message = new Message();
        message.setRoute_id(routeId);
        message.setProducer(SELF_NAME);
        message.setConsumer(firstVertex.getConsumer());
        message.setIs_received(false);
        message.setContent(Content.json(content));

        messageProducerService.sendMessage(firstVertex.getTopic(), message);
    }

    private void continueRouteById(Long routeId, int graphId, String currOperation, int currOperationResult, String nextOperation, String nextComponent, String nextTopic) {
        Content content = new Content(routeId, graphId, nextOperation);

        List<Operation> logList = content.getLog();
        logList.add(new Operation(routeId, currOperation, SELF_NAME, currOperationResult));
        content.setLog(logList);

        Message message = new Message();
        message.setRoute_id(routeId);
        message.setProducer(SELF_NAME);
        message.setConsumer(nextComponent);
        message.setIs_received(false);
        message.setContent(Content.json(content));

        messageProducerService.sendMessage(nextTopic, message);
    }

    private void continueRoute(Content content, int code) {
        long routeId = content.getRoute_id();
        int graphId = content.getGraph_id();
        String operation = content.getOperation();

        RouteVertex nextRouteVertex = innerRouteRepository.route(content.getGraph_id(), operation, code);
        if (!Objects.equals(nextRouteVertex.getGraph_id(), graphId)) {
            topicMessageRepository.deleteRouteMessages(routeId);
            stateRouteRepository.deleteRoute(routeId);
            createRouteById(nextRouteVertex.getGraph_id(), content);
        } else {
            continueRouteById(routeId, graphId, operation, code, nextRouteVertex.getOperation(), nextRouteVertex.getConsumer(), nextRouteVertex.getTopic());
        }
    }

    private void endRoute(Content content) {
        long routeId = content.getRoute_id();

        topicMessageRepository.deleteRouteMessages(routeId);
        stateRouteRepository.deleteRoute(routeId);
    }

    public void onRoute(Message message) {
        Content content = Message.getContentObject(message);
        String operation = content.getOperation();

        switch (operation) {
            case "TEST" -> {
                Action action = new Test();
                int code = action.execute(content);
                continueRoute(content, code);
            }
            case "END" -> {
                endRoute(content);
            }
        }
    }
}
