package io.github.tomaszfranczak.batchprocessor.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class TransactionBatchListener {

    private final TransactionService service;

    @KafkaListener(topics = "${app.kafka.topics.transactions.name}", batch = "true")
    void onBatch(List<Transaction> transactions) {
        service.store(transactions);
    }
}
