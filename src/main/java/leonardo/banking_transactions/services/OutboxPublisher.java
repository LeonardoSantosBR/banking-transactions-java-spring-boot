package leonardo.banking_transactions.services;

import leonardo.banking_transactions.entities.OutboxEventsEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxEventsService outboxEventsService;

    public OutboxPublisher(OutboxEventsService outboxEventsService) {
        this.outboxEventsService = outboxEventsService;
    }

    @Scheduled(fixedDelayString = "${aws.sqs.publisher.fixed-delay-ms:5000}")
    public void publishPendingEvents() {
        for (OutboxEventsEntity event : outboxEventsService.findPending(10)) {
            try {
                outboxEventsService.publish(event);
            } catch (RuntimeException exception) {
                log.error("Could not publish outbox event {}. It will remain PENDING for retry.", event.getId(), exception);
            }
        }
    }
}
