package leonardo.banking_transactions.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class AwsConfig {
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean(destroyMethod = "close")
    SqsClient sqsClient(@Value("${aws.region}") String region) {
        return SqsClient.builder().region(Region.of(region)).build();
    }

    @Bean(destroyMethod = "close")
    SnsClient snsClient(@Value("${aws.region}") String region) {
        return SnsClient.builder().region(Region.of(region)).build();
    }
}
