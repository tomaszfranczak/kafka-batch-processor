package io.github.tomaszfranczak.batchprocessor.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.kafka.topics.transactions")
record TransactionTopicProperties(
        @NotBlank String name,
        @Positive int partitions,
        @Positive int replicas) {
}
