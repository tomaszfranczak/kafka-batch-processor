package io.github.tomaszfranczak.batchprocessor.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class TransactionBatchListener {

    private final TransactionService service;

    @KafkaListener(topics = "${app.kafka.topics.transactions.name}", batch = "true")
    void onBatch(List<Transaction> transactions) {
        StoreResult result = service.store(transactions);
        log.info("Stored {} transactions, skipped {} duplicates", result.stored(), result.skippedDuplicates());
    }
}
