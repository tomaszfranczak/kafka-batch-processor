package io.github.tomaszfranczak.batchprocessor.transaction;

import io.github.tomaszfranczak.batchprocessor.IntegrationTest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.VALID_TRANSACTION_JSON;
import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.validTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

@IntegrationTest
class TransactionPublishingIT {

    private static final String TOPIC = "transactions";

    @Autowired
    private KafkaContainer kafka;

    @Autowired
    private MockMvcTester mvc;

    @Test
    void transactionPostedToTheApiReachesTheTopicKeyedByAccountId() {
        // when
        MvcTestResult result = mvc.post().uri("/api/transactions")
                .with(httpBasic("test-user", "test-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_TRANSACTION_JSON)
                .exchange();

        // then
        assertThat(result).hasStatus(HttpStatus.ACCEPTED);
        try (Consumer<String, Transaction> consumer = transactionsConsumer()) {
            consumer.subscribe(List.of(TOPIC));
            ConsumerRecord<String, Transaction> published =
                    KafkaTestUtils.getSingleRecord(consumer, TOPIC, Duration.ofSeconds(10));
            assertThat(published.key()).isEqualTo("account-1");
            assertThat(published.value()).isEqualTo(validTransaction().build());
        }
    }

    private Consumer<String, Transaction> transactionsConsumer() {
        Map<String, Object> properties = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "transaction-publishing-it",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(
                properties, new StringDeserializer(), new JacksonJsonDeserializer<>(Transaction.class))
                .createConsumer();
    }
}
