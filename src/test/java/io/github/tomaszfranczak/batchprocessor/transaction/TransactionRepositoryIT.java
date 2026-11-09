package io.github.tomaszfranczak.batchprocessor.transaction;

import io.github.tomaszfranczak.batchprocessor.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.validTransaction;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class TransactionRepositoryIT {

    @Autowired
    private TransactionRepository repository;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void deleteAllTransactions() {
        jdbc.sql("DELETE FROM transactions").update();
    }

    @Test
    void storedTransactionKeepsEveryField() {
        // given
        Transaction transaction = validTransaction().build();

        // when
        repository.saveAll(List.of(transaction));

        // then
        assertThat(storedTransactions()).containsExactly(transaction);
    }

    @Test
    void transactionWithAnAlreadyStoredIdIsSkipped() {
        // given
        Transaction original = validTransaction().transactionId("tx-1").price(new BigDecimal("100.00")).build();
        Transaction redelivered = original.toBuilder().price(new BigDecimal("999.00")).build();
        repository.saveAll(List.of(original));

        // when
        int stored = repository.saveAll(List.of(redelivered));

        // then
        assertThat(stored).isZero();
        assertThat(storedTransactions()).containsExactly(original);
    }

    @Test
    void duplicatesWithinOneBatchAreStoredOnce() {
        // given
        Transaction first = validTransaction().transactionId("tx-1").build();
        Transaction second = validTransaction().transactionId("tx-2").build();

        // when
        int stored = repository.saveAll(List.of(first, second, first));

        // then
        assertThat(stored).isEqualTo(2);
        assertThat(storedTransactions()).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void emptyBatchStoresNothing() {
        // when
        int stored = repository.saveAll(List.of());

        // then
        assertThat(stored).isZero();
        assertThat(storedTransactions()).isEmpty();
    }

    private List<Transaction> storedTransactions() {
        return jdbc.sql("""
                        SELECT transaction_id, order_id, account_id, symbol, side, price, quantity, execution_time
                        FROM transactions
                        """)
                .query((row, rowNumber) -> new Transaction(
                        row.getString("transaction_id"),
                        row.getString("order_id"),
                        row.getString("account_id"),
                        row.getString("symbol"),
                        Side.valueOf(row.getString("side")),
                        row.getBigDecimal("price"),
                        row.getBigDecimal("quantity"),
                        row.getObject("execution_time", OffsetDateTime.class).toInstant()))
                .list();
    }
}
