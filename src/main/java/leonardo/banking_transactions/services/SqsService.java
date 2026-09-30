package leonardo.banking_transactions.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import leonardo.banking_transactions.entities.OutboxEventsEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

@Service
public class SqsService {
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String transactionQueueUrl;

    public SqsService(SqsClient sqsClient, ObjectMapper objectMapper,
            @Value("${aws.sqs.transaction-queue-url}") String transactionQueueUrl) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.transactionQueueUrl = transactionQueueUrl;
    }

    public void sendTransactionEvent(OutboxEventsEntity event) {
        try {
            ObjectNode message = objectMapper.createObjectNode();
            message.put("eventId", event.getId().toString());
            message.put("eventType", event.getEventType());
            message.put("transactionId", event.getTransaction().getId().toString());
            message.put("idempotencyKey", event.getTransaction().getIdempotencyKey());
            message.putPOJO("createdAt", event.getCreatedAt());
            message.set("payload", event.getPayload());

            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(transactionQueueUrl)
                    .messageBody(objectMapper.writeValueAsString(message))
                    .messageGroupId(event.getTransaction().getPayerAccount().getId().toString())
                    .messageDeduplicationId(event.getId().toString())
                    .build());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize outbox event", exception);
        }
    }

    public List<Message> receiveTransactionEvents(int maxMessages, int waitTimeSeconds) {
        return sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(transactionQueueUrl)
                .maxNumberOfMessages(maxMessages)
                .waitTimeSeconds(waitTimeSeconds)
                .visibilityTimeout(60)
                .build()).messages();
    }

    public void deleteTransactionEvent(Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(transactionQueueUrl)
                .receiptHandle(message.receiptHandle())
                .build());
    }
}
