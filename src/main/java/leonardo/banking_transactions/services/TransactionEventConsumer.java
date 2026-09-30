package leonardo.banking_transactions.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class TransactionEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);
    private final SqsService sqsService;
    private final TransactionsService transactionsService;
    private final ObjectMapper objectMapper;

    public TransactionEventConsumer(SqsService sqsService, TransactionsService transactionsService,
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
        String transactionId = requiredText(root, "transactionId");
        boolean rejected = root.path("payload").path("reject").asBoolean(false);
        transactionsService.processSettlement(UUID.fromString(transactionId), rejected);
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required event field: " + field);
        }
        return value;
    }
}
