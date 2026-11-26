package io.github.tomaszfranczak.batchprocessor.transaction;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
class TransactionBatchListener {

    private final TransactionService service;
    private final Validator validator;

    @KafkaListener(topics = "${app.kafka.topics.transactions.name}", batch = "true")
    void onBatch(List<ConsumerRecord<String, Transaction>> records) {
        for (int index = 0; index < records.size(); index++) {
            Optional<String> problem = findProblem(records.get(index).value());
            if (problem.isPresent()) {
                store(records.subList(0, index));
                throw new BatchListenerFailedException(
                        problem.get(), new InvalidTransactionException(problem.get()), index);
            }
        }
        store(records);
    }

    private Optional<String> findProblem(Transaction transaction) {
        if (transaction == null) {
            return Optional.of("Message could not be read as a transaction");
        }
        String invalidFields = validator.validate(transaction).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .sorted()
                .collect(Collectors.joining(", "));
        return invalidFields.isEmpty()
                ? Optional.empty()
                : Optional.of("Transaction " + transaction.transactionId() + " has invalid fields: " + invalidFields);
    }

    private void store(List<ConsumerRecord<String, Transaction>> records) {
        if (records.isEmpty()) {
            return;
        }
        StoreResult result = service.store(records.stream().map(ConsumerRecord::value).toList());
        log.info("Stored {} transactions, skipped {} duplicates", result.stored(), result.skippedDuplicates());
    }
}
