package g3.rm.resourcemanager.init;

import g3.rm.resourcemanager.init.repositories.InitRepository;
import g3.rm.resourcemanager.init.services.UpdateParametersService;
import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.ExecutionException;


@Component
public class Init {

    private final KafkaAdmin kafkaAdmin;
    private final InitRepository initRepository;
    private final UpdateParametersService updateParametersService;
    private final Logger LOGGER = LogManager.getLogger(Init.class);

    private final String[] TOPICS = {"rqm-topic", "qm-topic", "rm-topic"};

    public Init(KafkaAdmin kafkaAdmin,
                InitRepository initRepository,
                UpdateParametersService updateParametersService) {
        this.kafkaAdmin = kafkaAdmin;
        this.initRepository = initRepository;
        this.updateParametersService = updateParametersService;
    }

    @PostConstruct
    public void InitProcedure() {
        createKafkaTopics();
        initRepository.initSelfRepository();
        updateParametersService.updateSelfParams();
    }

    private void createKafkaTopics() {
        for (String topicName : TOPICS) {
            LOGGER.info("Trying to create topic: " + topicName);

            try (AdminClient admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())){
                ListTopicsResult listTopicsResult = admin.listTopics();
                if (listTopicsResult.names().get().contains(topicName)) {
                    LOGGER.info("Topic with name " + topicName + " already exists");
                    continue;
                }

                LOGGER.info("Creating new topic: " + topicName);

                NewTopic newTopic = TopicBuilder.name(topicName)
                        .partitions(10)
                        .replicas(3)
                        .config(TopicConfig.RETENTION_MS_CONFIG, "259200000")
                        .build();

                CreateTopicsResult createTopicsResult = admin.createTopics(Collections.singleton(newTopic));
                KafkaFuture<Void> future = createTopicsResult.values().get(topicName);
                future.get();

                LOGGER.info(newTopic.name() + " created");

            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
