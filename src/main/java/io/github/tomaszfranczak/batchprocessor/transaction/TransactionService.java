package io.github.tomaszfranczak.batchprocessor.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
class TransactionService {

    private final TransactionRepository repository;

    @Transactional
    StoreResult store(List<Transaction> transactions) {
        int stored = repository.saveAll(transactions);
        return new StoreResult(stored, transactions.size() - stored);
    }
}
