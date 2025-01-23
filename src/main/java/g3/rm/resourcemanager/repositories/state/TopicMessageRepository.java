package g3.rm.resourcemanager.repositories.state;

import g3.rm.resourcemanager.message.KafkaMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.UUID;

@Repository
public class TopicMessageRepository {
    private final JdbcTemplate jdbcTemplate;

    public TopicMessageRepository(@Qualifier("stateDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public int saveMessage(KafkaMessage message) {
        return jdbcTemplate.update("""
                INSERT INTO state_schema.topic_message (unique_id, route_id, producer_component, consumer_component, is_received, content)
                VALUES (?, ?, ?, ?, ?, ?::JSON)
                """
                , message.getUnique_id(), message.getRoute_id(), message.getProducer(), message.getConsumer(), false, message.getContent());
    }

    public KafkaMessage getMessage(UUID message_uuid) {
        return jdbcTemplate.queryForObject("""
                SELECT * FROM state_schema.topic_message
                WHERE unique_id = ?
                """
                , (rs, rowNum) -> {
                    KafkaMessage message = new KafkaMessage();
                    message.setUnique_id(UUID.fromString(rs.getString("unique_id")));
                    message.setRoute_id(rs.getLong("route_id"));
                    message.setProducer(rs.getString("producer_component"));
                    message.setConsumer(rs.getString("consumer_component"));
                    message.setIs_received(rs.getBoolean("is_received"));
                    message.setContent(rs.getString("content"));
                    return message;
                }, message_uuid);
    }

    public int commitReceiveMessage(UUID message_uuid) {
        return jdbcTemplate.update("""
                UPDATE state_schema.topic_message set is_received = true
                WHERE unique_id = ?
                """
                , message_uuid);
    }
}
