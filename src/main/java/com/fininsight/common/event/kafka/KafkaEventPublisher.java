package com.fininsight.common.event.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Low-level Kafka publisher for sending raw JSON payloads to designated topics.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Publishes a message to a Kafka topic asynchronously.
     *
     * @param topic the target Kafka topic
     * @param key the message/partition key
     * @param payload the JSON payload string
     * @return CompletableFuture of SendResult
     */
    public CompletableFuture<SendResult<String, String>> send(String topic, String key, String payload) {
        log.debug("Sending message to Kafka topic '{}' with key '{}'", topic, key);
        return kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish message to topic '{}' with key '{}': {}",
                                topic, key, ex.getMessage());
                    } else if (result != null) {
                        log.debug("Message successfully sent to topic '{}' [partition={}, offset={}]",
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
