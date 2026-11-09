package io.github.tomaszfranczak.batchprocessor.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class TransactionService {

    private final TransactionRepository repository;

    @Transactional
    StoreResult store(List<Transaction> transactions) {
        int stored = repository.saveAll(transactions);
        StoreResult result = new StoreResult(stored, transactions.size() - stored);
        log.info("Stored {} transactions, skipped {} duplicates", result.stored(), result.skippedDuplicates());
        return result;
    }
}
