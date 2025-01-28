package g3.rm.resourcemanager.configs;

import g3.rm.resourcemanager.repositories.AutorunRepository;
import g3.rm.resourcemanager.services.UpdateParametersService;
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
public class Autorun {

    private final KafkaAdmin kafkaAdmin;
    private final AutorunRepository autorunRepository;
    private final UpdateParametersService updateParametersService;
    private final Logger LOGGER = LogManager.getLogger(Autorun.class);

    private final String[] TOPICS = {"qm-topic", "rm-topic"};

    public Autorun(KafkaAdmin kafkaAdmin,
                   AutorunRepository autorunRepository,
                   UpdateParametersService updateParametersService) {
        this.kafkaAdmin = kafkaAdmin;
        this.autorunRepository = autorunRepository;
        this.updateParametersService = updateParametersService;
    }

    @PostConstruct
    public void Init() {
        createKafkaTopics();
        autorunRepository.initSelfRepository();
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
