package com.eshop.app.core.outbox.processor;

import com.eshop.app.core.outbox.entity.OutboxEvent;
import com.eshop.app.core.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxEventProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelayString = "${app.outbox.processor.delay-ms:5000}")
    @Transactional
    public void processPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Processing {} pending outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // In a production system with message brokers, we would publish to Kafka/RabbitMQ here.
                // For this monolith, we deserialize and publish to the local Spring event publisher context.
                log.debug("Publishing outbox event: type={}, correlationId={}", event.getEventType(), event.getCorrelationId());
                
                event.setProcessed(true);
                event.setProcessedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to process outbox event: id={}, type={}", event.getId(), event.getEventType(), e);
            }
        }
    }
}
