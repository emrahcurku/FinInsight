package com.fininsight.event;

import com.fininsight.common.event.kafka.DeadLetterQueueConsumer;
import com.fininsight.config.kafka.KafkaTopicNames;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;

public class DeadLetterQueueConsumerTest {

    private DeadLetterQueueConsumer dltConsumer;

    @BeforeEach
    public void setUp() {
        dltConsumer = new DeadLetterQueueConsumer();
    }

    @Test
    @DisplayName("DeadLetterQueueConsumer handles DLT records without error")
    public void testConsumeDltRecord() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                KafkaTopicNames.TRANSACTION_EVENTS_DLT,
                0,
                10L,
                "key-1",
                "{\"eventId\":\"test-event\"}"
        );

        assertThatNoException().isThrownBy(() -> dltConsumer.consumeDlt(record));
    }
}
