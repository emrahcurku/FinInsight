package com.fininsight.event;

import com.fininsight.common.event.kafka.KafkaEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private KafkaEventPublisher kafkaEventPublisher;

    @BeforeEach
    public void setUp() {
    }

    @Test
    @DisplayName("KafkaEventPublisher delegates send to KafkaTemplate")
    public void testSendDelegatesToKafkaTemplate() {
        String topic = "test.topic";
        String key = "test-key";
        String payload = "{\"test\":true}";

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(eq(topic), eq(key), eq(payload))).thenReturn(future);

        CompletableFuture<SendResult<String, String>> result = kafkaEventPublisher.send(topic, key, payload);

        assertThat(result).isNotNull();
        verify(kafkaTemplate).send(topic, key, payload);
    }
}
