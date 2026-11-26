package io.github.tomaszfranczak.batchprocessor.transaction;

import io.github.tomaszfranczak.batchprocessor.IntegrationTest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.tomaszfranczak.batchprocessor.transaction.StoredTransactions.withIds;
import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.VALID_TRANSACTION_JSON;
import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.uniqueId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class TransactionDeadLetterIT {

    private static final String TOPIC = "transactions";
    private static final String DEAD_LETTER_TOPIC = "transactions.DLT";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    @Autowired
    private KafkaContainer kafka;

    @Autowired
    private JdbcClient jdbc;

    private KafkaTemplate<String, String> rawTemplate;

    @BeforeEach
    void createRawTemplate() {
        rawTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)));
    }

    @Test
    void unreadableMessageGoesToTheDeadLetterTopicAndTheNextTransactionIsStored() {
        // given
        String accountId = uniqueId("account");
        String validId = uniqueId("tx");

        // when
        send(accountId, "{ \"transactionId\": ");
        send(accountId, json(validId, accountId, "67250.50"));

        // then
        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(withIds(jdbc, validId)).hasSize(1));
        assertThat(deadLetter(accountId).value()).isEqualTo("{ \"transactionId\": ");
    }

    @Test
    void invalidTransactionGoesToTheDeadLetterTopicAndTheNextTransactionIsStored() {
        // given
        String accountId = uniqueId("account");
        String invalidId = uniqueId("tx");
        String validId = uniqueId("tx");

        // when
        send(accountId, json(invalidId, accountId, "-1"));
        send(accountId, json(validId, accountId, "67250.50"));

        // then
        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(withIds(jdbc, validId)).hasSize(1));
        assertThat(withIds(jdbc, invalidId)).isEmpty();
        assertThat(deadLetter(accountId).value()).contains(invalidId);
    }

    private void send(String accountId, String value) {
        rawTemplate.send(TOPIC, accountId, value).join();
    }

    private static String json(String transactionId, String accountId, String price) {
        return VALID_TRANSACTION_JSON
                .replace("tx-1", transactionId)
                .replace("account-1", accountId)
                .replace("67250.50", price);
    }

    private ConsumerRecord<String, String> deadLetter(String accountId) {
        List<ConsumerRecord<String, String>> received = new ArrayList<>();
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, uniqueId("dead-letter-it"),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            consumer.subscribe(List.of(DEAD_LETTER_TOPIC));
            await().atMost(TIMEOUT).pollInSameThread().until(() -> {
                consumer.poll(Duration.ofMillis(200)).forEach(received::add);
                return received.stream().anyMatch(record -> accountId.equals(record.key()));
            });
        }
        return received.stream().filter(record -> accountId.equals(record.key())).findFirst().orElseThrow();
    }
}
