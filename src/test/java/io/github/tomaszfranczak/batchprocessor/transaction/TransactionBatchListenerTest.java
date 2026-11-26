package io.github.tomaszfranczak.batchprocessor.transaction;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.BatchListenerFailedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.validTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TransactionBatchListenerTest {

    private static ValidatorFactory validatorFactory;

    @Mock
    private TransactionService service;

    private TransactionBatchListener listener;

    @BeforeAll
    static void createValidatorFactory() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @BeforeEach
    void createListener() {
        listener = new TransactionBatchListener(service, validatorFactory.getValidator());
    }

    @Test
    void validBatchIsStoredAtOnce() {
        // given
        Transaction first = validTransaction().transactionId("tx-1").build();
        Transaction second = validTransaction().transactionId("tx-2").build();
        given(service.store(anyList())).willReturn(new StoreResult(2, 0));

        // when
        listener.onBatch(records(first, second));

        // then
        verify(service).store(List.of(first, second));
    }

    @Test
    void invalidTransactionFailsTheBatchAtItsIndexAfterStoringThePrecedingOnes() {
        // given
        Transaction first = validTransaction().transactionId("tx-1").build();
        Transaction second = validTransaction().transactionId("tx-2").build();
        Transaction invalid = validTransaction().transactionId("tx-3").price(new BigDecimal("-1")).build();
        Transaction fourth = validTransaction().transactionId("tx-4").build();
        given(service.store(anyList())).willReturn(new StoreResult(2, 0));

        // when
        BatchListenerFailedException failure = catchThrowableOfType(BatchListenerFailedException.class,
                () -> listener.onBatch(records(first, second, invalid, fourth)));

        // then
        verify(service).store(List.of(first, second));
        assertThat(failure.getIndex()).isEqualTo(2);
        assertThat(failure).hasCauseInstanceOf(InvalidTransactionException.class);
        assertThat(failure.getCause()).hasMessageContaining("price");
    }

    @Test
    void unreadableMessageFailsTheBatchAtItsIndex() {
        // given
        Transaction first = validTransaction().transactionId("tx-1").build();
        given(service.store(anyList())).willReturn(new StoreResult(1, 0));

        // when
        BatchListenerFailedException failure = catchThrowableOfType(BatchListenerFailedException.class,
                () -> listener.onBatch(records(first, null)));

        // then
        verify(service).store(List.of(first));
        assertThat(failure.getIndex()).isEqualTo(1);
        assertThat(failure).hasCauseInstanceOf(InvalidTransactionException.class);
    }

    @Test
    void invalidFirstTransactionStoresNothing() {
        // given
        Transaction invalid = validTransaction().transactionId("tx-1").symbol("btc").build();
        Transaction valid = validTransaction().transactionId("tx-2").build();

        // when
        BatchListenerFailedException failure = catchThrowableOfType(BatchListenerFailedException.class,
                () -> listener.onBatch(records(invalid, valid)));

        // then
        assertThat(failure.getIndex()).isZero();
        verifyNoInteractions(service);
    }

    private static List<ConsumerRecord<String, Transaction>> records(Transaction... transactions) {
        return IntStream.range(0, transactions.length)
                .mapToObj(offset -> new ConsumerRecord<>("transactions", 0, offset, "account-1", transactions[offset]))
                .toList();
    }
}
