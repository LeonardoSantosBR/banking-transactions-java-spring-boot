package leonardo.banking_transactions.service.aws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import leonardo.banking_transactions.entity.OutboxEventsEntity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Service
public class SnsService {
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final String transactionTopicArn;

    public SnsService(SnsClient snsClient, ObjectMapper objectMapper,
            @Value("${aws.sns.transaction-topic-arn}") String transactionTopicArn) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.transactionTopicArn = transactionTopicArn;
    }

    public void publishTransactionEvent(OutboxEventsEntity event) {
        if (transactionTopicArn == null || transactionTopicArn.isBlank()) {
            throw new IllegalStateException("AWS SNS transaction topic ARN is not configured");
        }
        try {
            ObjectNode message = objectMapper.createObjectNode();
            message.put("eventId", event.getId().toString());
            message.put("eventType", event.getEventType());
            message.put("transactionId", event.getTransaction().getId().toString());
            message.put("idempotencyKey", event.getTransaction().getIdempotencyKey());
            message.put("createdAt", event.getCreatedAt().toString());
            message.set("payload", event.getPayload());
            snsClient.publish(PublishRequest.builder()
                    .topicArn(transactionTopicArn)
                    .message(objectMapper.writeValueAsString(message))
                    .build());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize outbox event for SNS", exception);
        }
    }
}
