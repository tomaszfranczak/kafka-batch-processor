package io.github.tomaszfranczak.batchprocessor.transaction;

import io.github.tomaszfranczak.batchprocessor.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.util.List;

import static io.github.tomaszfranczak.batchprocessor.transaction.StoredTransactions.withIds;
import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.uniqueId;
import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.validTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class TransactionRepositoryIT {

    @Autowired
    private TransactionRepository repository;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void storedTransactionKeepsEveryField() {
        // given
        Transaction transaction = validTransaction().transactionId(uniqueId("tx")).build();

        // when
        repository.saveAll(List.of(transaction));

        // then
        assertThat(withIds(jdbc, transaction.transactionId())).containsExactly(transaction);
    }

    @Test
    void transactionWithAnAlreadyStoredIdIsSkipped() {
        // given
        Transaction original = validTransaction().transactionId(uniqueId("tx")).price(new BigDecimal("100.00")).build();
        Transaction redelivered = original.toBuilder().price(new BigDecimal("999.00")).build();
        repository.saveAll(List.of(original));

        // when
        int stored = repository.saveAll(List.of(redelivered));

        // then
        assertThat(stored).isZero();
        assertThat(withIds(jdbc, original.transactionId())).containsExactly(original);
    }

    @Test
    void duplicatesWithinOneBatchAreStoredOnce() {
        // given
        Transaction first = validTransaction().transactionId(uniqueId("tx")).build();
        Transaction second = validTransaction().transactionId(uniqueId("tx")).build();

        // when
        int stored = repository.saveAll(List.of(first, second, first));

        // then
        assertThat(stored).isEqualTo(2);
        assertThat(withIds(jdbc, first.transactionId(), second.transactionId()))
                .containsExactlyInAnyOrder(first, second);
    }

    @Test
    void emptyBatchStoresNothing() {
        // when
        int stored = repository.saveAll(List.of());

        // then
        assertThat(stored).isZero();
    }

    @Test
    void batchWithATransactionRejectedByTheDatabaseStoresNothing() {
        // given
        Transaction valid = validTransaction().transactionId(uniqueId("tx")).build();
        Transaction rejected = validTransaction().transactionId(uniqueId("tx")).price(new BigDecimal("-1")).build();

        // when & then
        assertThatThrownBy(() -> repository.saveAll(List.of(valid, rejected)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(withIds(jdbc, valid.transactionId(), rejected.transactionId())).isEmpty();
    }
}
