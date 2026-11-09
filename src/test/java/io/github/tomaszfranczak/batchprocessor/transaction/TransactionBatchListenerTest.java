package io.github.tomaszfranczak.batchprocessor.transaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.validTransaction;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionBatchListenerTest {

    @Mock
    private TransactionService service;

    @InjectMocks
    private TransactionBatchListener listener;

    @Test
    void receivedBatchIsStored() {
        // given
        List<Transaction> batch = List.of(
                validTransaction().transactionId("tx-1").build(),
                validTransaction().transactionId("tx-2").build());
        given(service.store(batch)).willReturn(new StoreResult(2, 0));

        // when
        listener.onBatch(batch);

        // then
        verify(service).store(batch);
    }
}
