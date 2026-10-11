package com.b2b.warehouse.async;

import com.b2b.warehouse.outbox.entity.OutboxEventEntity;
import com.b2b.warehouse.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transactional Outbox Relay Worker protected by ShedLock.
 * Polls {@code outbox_events} ({@code WHERE status = 'PENDING' ORDER BY created_at ASC})
 * and publishes domain events to Apache Kafka ({@code b2b.domain.events}) with partition key {@code aggregateId}
 * to guarantee strict per-order event ordering and at-least-once delivery.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventRelayScheduler {

    private static final int MAX_RETRIES = 5;
    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final DomainEventNotificationProcessor notificationProcessor;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.domain-events-topic:b2b.domain.events}")
    private String domainEventsTopic;

    @Value("${app.kafka.consumer-enabled:false}")
    private boolean kafkaConsumerEnabled;

    @Scheduled(fixedDelayString = "${app.cron.outbox-relay-delay-ms:5000}")
    @SchedulerLock(
            name = "outboxEventRelayLock",
            lockAtLeastFor = "PT1S",
            lockAtMostFor = "PT30S"
    )
    @Transactional
    public int relayPendingOutboxEvents() {
        List<OutboxEventEntity> pendingEvents = outboxEventRepository.findPendingEventsBatch(
                PageRequest.of(0, BATCH_SIZE)
        );
        if (pendingEvents.isEmpty()) {
            return 0;
        }

        int publishedCount = 0;
        for (OutboxEventEntity event : pendingEvents) {
            try {
                Map<String, Object> envelope = new LinkedHashMap<>();
                envelope.put("eventId", event.getId());
                envelope.put("aggregateType", event.getAggregateType());
                envelope.put("aggregateId", event.getAggregateId());
                envelope.put("eventType", event.getEventType());
                envelope.put("payload", event.getPayload());
                envelope.put("createdAt", event.getCreatedAt());

                String jsonEnvelope = objectMapper.writeValueAsString(envelope);

                if (kafkaConsumerEnabled) {
                    kafkaTemplate.send(domainEventsTopic, event.getAggregateId(), jsonEnvelope);
                } else {
                    notificationProcessor.processEvent(
                            event.getEventType(),
                            event.getAggregateId(),
                            event.getPayload()
                    );
                }

                event.setStatus("PUBLISHED");
                event.setPublishedAt(Instant.now());
                publishedCount++;
            } catch (Exception ex) {
                int nextRetry = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
                event.setRetryCount(nextRetry);
                if (nextRetry >= MAX_RETRIES) {
                    event.setStatus("FAILED");
                    log.error("Outbox event {} exceeded max retries ({}) and marked FAILED",
                            event.getId(), MAX_RETRIES, ex);
                } else {
                    log.warn("Transient failure relaying outbox event {} (attempt {}/{})",
                            event.getId(), nextRetry, MAX_RETRIES);
                }
            }
        }

        outboxEventRepository.saveAll(pendingEvents);
        return publishedCount;
    }
}
