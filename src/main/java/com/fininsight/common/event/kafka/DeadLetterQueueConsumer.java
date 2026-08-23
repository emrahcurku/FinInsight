package com.fininsight.common.event.kafka;

import com.fininsight.config.kafka.KafkaTopicNames;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for Dead Letter Topics (DLT).
 * Logs unprocessable or repeatedly failed messages for audit and debugging purposes.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class DeadLetterQueueConsumer {

    @KafkaListener(
            topics = {
                    KafkaTopicNames.TRANSACTION_EVENTS_DLT,
                    KafkaTopicNames.BUDGET_EVENTS_DLT,
                    KafkaTopicNames.CATEGORY_EVENTS_DLT
            },
            groupId = "fininsight-dlt-consumer-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeDlt(ConsumerRecord<String, String> record) {
        log.error("Received message in Dead Letter Topic '{}' [key='{}', partition={}, offset={}].",
                record.topic(), record.key(), record.partition(), record.offset());
    }
}
