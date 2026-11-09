package io.github.tomaszfranczak.batchprocessor.transaction;

import io.github.tomaszfranczak.batchprocessor.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import static io.github.tomaszfranczak.batchprocessor.transaction.StoredTransactions.withIds;
import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.VALID_TRANSACTION_JSON;
import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.uniqueId;
import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.validTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

@IntegrationTest
class TransactionProcessingIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private KafkaTemplate<String, Transaction> kafkaTemplate;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void transactionPostedToTheApiIsStored() {
        // given
        Transaction transaction = validTransaction().transactionId(uniqueId("tx")).build();

        // when
        MvcTestResult result = mvc.post().uri("/api/transactions")
                .with(httpBasic("test-user", "test-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_TRANSACTION_JSON.replace("tx-1", transaction.transactionId()))
                .exchange();

        // then
        assertThat(result).hasStatus(HttpStatus.ACCEPTED);
        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(withIds(jdbc, transaction.transactionId())).containsExactly(transaction));
    }

    @Test
    void everyTransactionIsStoredOnceEvenWhenDeliveredTwice() {
        // given
        List<Transaction> transactions = IntStream.range(0, 50)
                .mapToObj(index -> validTransaction()
                        .transactionId(uniqueId("tx"))
                        .accountId("account-" + index % 5)
                        .build())
                .toList();
        String[] ids = transactions.stream().map(Transaction::transactionId).toArray(String[]::new);

        // when
        transactions.forEach(this::send);
        transactions.subList(0, 10).forEach(this::send);

        // then
        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(withIds(jdbc, ids)).containsExactlyInAnyOrderElementsOf(transactions));
    }

    private void send(Transaction transaction) {
        kafkaTemplate.send("transactions", transaction.accountId(), transaction).join();
    }
}
