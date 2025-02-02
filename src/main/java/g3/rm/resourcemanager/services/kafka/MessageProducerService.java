package g3.rm.resourcemanager.services.kafka;

import g3.rm.resourcemanager.dtos.kafka.Message;
import g3.rm.resourcemanager.repositories.state.TopicMessageRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MessageProducerService {
    private final TopicMessageRepository topicMessageRepository;
    private final KafkaTemplate<Long, UUID> kafkaProducerTemplate;
    private final Logger LOGGER = LogManager.getLogger(MessageProducerService.class);


    public MessageProducerService(TopicMessageRepository topicMessageRepository, KafkaTemplate<Long, UUID> kafkaProducerTemplate) {
        this.topicMessageRepository = topicMessageRepository;
        this.kafkaProducerTemplate = kafkaProducerTemplate;
    }

    public void sendMessage(String topicName, Message message) {
        long key = message.getRoute_id();
        try {
            int updateCount = topicMessageRepository
                    .saveMessage(message);

            if (updateCount != 0) {
                kafkaProducerTemplate
                        .send(new ProducerRecord<>(topicName, key, message.getUnique_id()))
                        .whenComplete((metadata, error) -> {
                            if (error == null) {
                                //commit message and offset
                                LOGGER.info("Message sent: " + message.getUnique_id() + ", metadata: " + metadata);
                            } else {
                                //save message -> add message into inner queue for next try
                            }
                        });
            }

        } catch (DataAccessException ex) {
            //save query -> add message into inner queue for next try
            LOGGER.error("Error while sent message: " + message, ex);
        }
    }
}
