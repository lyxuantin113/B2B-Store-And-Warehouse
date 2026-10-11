package com.b2b.warehouse.async;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Distributed Apache Kafka consumer for B2B domain events ({@code b2b.domain.events}).
 * Enabled when {@code app.kafka.consumer-enabled=true} in multi-node production deployments.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.consumer-enabled", havingValue = "true")
public class KafkaDomainEventConsumer {

    private final ObjectMapper objectMapper;
    private final DomainEventNotificationProcessor notificationProcessor;

    @KafkaListener(
            topics = "${app.kafka.domain-events-topic:b2b.domain.events}",
            groupId = "${spring.kafka.consumer.group-id:b2b-warehouse-group}"
    )
    public void onDomainEventMessage(String messageJson) {
        try {
            Map<String, Object> envelope = objectMapper.readValue(
                    messageJson,
                    new TypeReference<Map<String, Object>>() {}
            );
            String eventType = String.valueOf(envelope.getOrDefault("eventType", ""));
            String aggregateId = String.valueOf(envelope.getOrDefault("aggregateId", ""));
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");

            notificationProcessor.processEvent(eventType, aggregateId, payload);
        } catch (Exception ex) {
            log.error("Failed to process Kafka domain event message: {}", messageJson, ex);
        }
    }
}
