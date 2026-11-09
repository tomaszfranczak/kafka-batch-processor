package io.github.tomaszfranczak.batchprocessor.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.validTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    private TransactionService service;

    @BeforeEach
    void createService() {
        service = new TransactionService(repository);
    }

    @Test
    void wholeBatchIsStoredAtOnce() {
        // given
        List<Transaction> batch = List.of(
                validTransaction().transactionId("tx-1").build(),
                validTransaction().transactionId("tx-2").build());

        // when
        service.store(batch);

        // then
        verify(repository).saveAll(batch);
    }

    @Test
    void transactionsNotStoredByTheRepositoryAreReportedAsDuplicates() {
        // given
        List<Transaction> batch = List.of(
                validTransaction().transactionId("tx-1").build(),
                validTransaction().transactionId("tx-2").build(),
                validTransaction().transactionId("tx-3").build());
        given(repository.saveAll(anyList())).willReturn(2);

        // when
        StoreResult result = service.store(batch);

        // then
        assertThat(result).isEqualTo(new StoreResult(2, 1));
    }
}
