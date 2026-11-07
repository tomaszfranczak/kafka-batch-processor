package io.github.tomaszfranczak.batchprocessor.transaction;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
class TransactionController {

    private final TransactionPublisher publisher;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    void publish(@Valid @RequestBody Transaction transaction) {
        publisher.publish(transaction);
    }
}
