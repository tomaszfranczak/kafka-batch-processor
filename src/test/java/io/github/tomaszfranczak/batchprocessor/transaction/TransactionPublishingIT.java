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
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.VALID_TRANSACTION_JSON;
import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.uniqueId;
import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.validTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
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
        // given
        Transaction transaction = validTransaction()
                .transactionId(uniqueId("tx"))
                .accountId(uniqueId("account"))
                .build();

        // when
        MvcTestResult result = mvc.post().uri("/api/transactions")
                .with(httpBasic("test-user", "test-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_TRANSACTION_JSON
                        .replace("tx-1", transaction.transactionId())
                        .replace("account-1", transaction.accountId()))
                .exchange();

        // then
        assertThat(result).hasStatus(HttpStatus.ACCEPTED);
        ConsumerRecord<String, Transaction> published = findPublished(transaction.transactionId());
        assertThat(published.key()).isEqualTo(transaction.accountId());
        assertThat(published.value()).isEqualTo(transaction);
    }

    private ConsumerRecord<String, Transaction> findPublished(String transactionId) {
        List<ConsumerRecord<String, Transaction>> received = new ArrayList<>();
        try (Consumer<String, Transaction> consumer = transactionsConsumer()) {
            consumer.subscribe(List.of(TOPIC));
            await().atMost(Duration.ofSeconds(10)).pollInSameThread().until(() -> {
                consumer.poll(Duration.ofMillis(200)).forEach(received::add);
                return received.stream().anyMatch(record -> hasId(record, transactionId));
            });
        }
        return received.stream()
                .filter(record -> hasId(record, transactionId))
                .findFirst()
                .orElseThrow();
    }

    private static boolean hasId(ConsumerRecord<String, Transaction> record, String transactionId) {
        return record.value() != null && record.value().transactionId().equals(transactionId);
    }

    private Consumer<String, Transaction> transactionsConsumer() {
        Map<String, Object> properties = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, uniqueId("transaction-publishing-it"),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new JacksonJsonDeserializer<>(Transaction.class)))
                .createConsumer();
    }
}
