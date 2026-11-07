package io.github.tomaszfranczak.batchprocessor.transaction;

import io.github.tomaszfranczak.batchprocessor.kafka.TransactionTopicProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TransactionPublisher {

    private final KafkaTemplate<String, Transaction> kafkaTemplate;
    private final TransactionTopicProperties topic;

    void publish(Transaction transaction) {
        try {
            kafkaTemplate.send(topic.name(), transaction.accountId(), transaction).join();
        } catch (RuntimeException e) {
            throw new TransactionPublishException(transaction.transactionId(), e);
        }
    }
}
