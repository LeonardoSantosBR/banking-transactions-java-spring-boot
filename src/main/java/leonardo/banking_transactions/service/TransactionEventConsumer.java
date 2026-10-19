package leonardo.banking_transactions.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import leonardo.banking_transactions.service.aws.SqsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class TransactionEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);
    private final SqsService sqsService;
    private final TransactionsService transactionsService;
    private final ObjectMapper objectMapper;

    public TransactionEventConsumer(
            SqsService sqsService,
            TransactionsService transactionsService,
            ObjectMapper objectMapper) {
        this.sqsService = sqsService;
        this.transactionsService = transactionsService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${aws.sqs.consumer.fixed-delay-ms:5000}")
    public void consumeTransactionEvents() {
        List<Message> messages = sqsService.receiveTransactionEvents(10, 10);
        for (Message message : messages) {
            try {
                process(message);
                sqsService.deleteTransactionEvent(message);
            } catch (RuntimeException | IOException exception) {
                log.error("Could not process SQS transaction message {}. It will be retried.",
                        message.messageId(), exception);
            }
        }
    }

    private void process(Message message) throws IOException {
        JsonNode root = objectMapper.readTree(message.body());
        UUID eventId = requiredUuid(root, "eventId");
        String eventType = requiredText(root, "eventType");
        if (!"PixTransactionRequested".equals(eventType) && !"PixTransactionRefundRequested".equals(eventType))
            throw new IllegalArgumentException("Unsupported event type");
        UUID transactionId = requiredUuid(root, "transactionId");
        String idempotencyKey = requiredText(root, "idempotencyKey");
        validateCreatedAt(root);
        JsonNode payload = root.path("payload");
        if (!payload.isObject() || !payload.path("amount").isNumber()
                || payload.path("amount").decimalValue().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Invalid transaction payload");
        if (!payload.path("currency").isTextual()
                || !payload.path("payerAccountId").isTextual()
                || !payload.path("payeeAccountId").isTextual())
            throw new IllegalArgumentException("Incomplete transaction payload");
        if (payload.has("reject") && !payload.path("reject").isBoolean())
            throw new IllegalArgumentException("Invalid reject flag");
        transactionsService.validateConsistencyMessageSQS(eventId, transactionId, idempotencyKey, payload, eventType);
    }

    private void validateCreatedAt(JsonNode node) {
        JsonNode createdAt = node.get("createdAt");
        try {
            if (createdAt != null && createdAt.isNumber()) {
                OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli((long) (createdAt.asDouble() * 1000)), java.time.ZoneOffset.UTC);
            } else {
                OffsetDateTime.parse(requiredText(node, "createdAt"));
            }
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid createdAt", exception);
        }
    }

    private UUID requiredUuid(JsonNode node, String field) {
        try {
            return UUID.fromString(requiredText(node, field));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid UUID field: " + field, e);
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Missing required event field: " + field);
        return value;
    }
}
