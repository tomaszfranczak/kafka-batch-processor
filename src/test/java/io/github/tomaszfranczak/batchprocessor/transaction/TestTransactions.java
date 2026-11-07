package io.github.tomaszfranczak.batchprocessor.transaction;

import java.math.BigDecimal;
import java.time.Instant;

public final class TestTransactions {

    private TestTransactions() {
    }

    public static Transaction.TransactionBuilder validTransaction() {
        return Transaction.builder()
                .transactionId("tx-1")
                .orderId("order-1")
                .accountId("account-1")
                .symbol("BTC-USDT")
                .side(Side.BUY)
                .price(new BigDecimal("67250.50"))
                .quantity(new BigDecimal("0.015"))
                .executionTime(Instant.parse("2026-11-07T12:00:00Z"));
    }
}
