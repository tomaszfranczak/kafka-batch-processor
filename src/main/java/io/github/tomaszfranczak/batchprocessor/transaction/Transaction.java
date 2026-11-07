package io.github.tomaszfranczak.batchprocessor.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder(toBuilder = true)
public record Transaction(
        @NotBlank String transactionId,
        @NotBlank String orderId,
        @NotBlank String accountId,
        @NotNull @Pattern(regexp = "[A-Z0-9]+-[A-Z0-9]+") String symbol,
        @NotNull Side side,
        @NotNull @Positive BigDecimal price,
        @NotNull @Positive BigDecimal quantity,
        @NotNull Instant executionTime) {
}
