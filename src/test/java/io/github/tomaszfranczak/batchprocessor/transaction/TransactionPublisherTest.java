package io.github.tomaszfranczak.batchprocessor.transaction;

import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static io.github.tomaszfranczak.batchprocessor.transaction.TestTransactions.validTransaction;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionPublisherTest {

    private static final String TOPIC = "transactions";

    @Mock
    private KafkaTemplate<String, Transaction> kafkaTemplate;

    private TransactionPublisher publisher;

    @BeforeEach
    void createPublisher() {
        publisher = new TransactionPublisher(kafkaTemplate, new TransactionTopicProperties(TOPIC, 3, 1));
    }

    @Test
    void transactionIsSentToTheConfiguredTopicKeyedByAccountId() {
        // given
        Transaction transaction = validTransaction().accountId("account-42").build();
        given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(CompletableFuture.completedFuture(null));

        // when
        publisher.publish(transaction);

        // then
        verify(kafkaTemplate).send(TOPIC, "account-42", transaction);
    }

    @Test
    void transactionNotAcknowledgedByTheBrokerIsReportedAsPublishFailure() {
        // given
        Transaction transaction = validTransaction().transactionId("tx-7").build();
        given(kafkaTemplate.send(anyString(), anyString(), any()))
                .willReturn(CompletableFuture.failedFuture(new KafkaException("broker unavailable")));

        // when & then
        assertThatThrownBy(() -> publisher.publish(transaction))
                .isInstanceOf(TransactionPublishException.class)
                .hasMessageContaining("tx-7")
                .hasRootCauseMessage("broker unavailable");
    }

    @Test
    void sendFailingImmediatelyIsReportedAsPublishFailure() {
        // given
        Transaction transaction = validTransaction().transactionId("tx-8").build();
        given(kafkaTemplate.send(anyString(), anyString(), any())).willThrow(new KafkaException("metadata timeout"));

        // when & then
        assertThatThrownBy(() -> publisher.publish(transaction))
                .isInstanceOf(TransactionPublishException.class)
                .hasMessageContaining("tx-8")
                .hasRootCauseMessage("metadata timeout");
    }
}
