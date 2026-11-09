package io.github.tomaszfranczak.batchprocessor.transaction;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.OffsetDateTime;
import java.util.List;

final class StoredTransactions {

    private StoredTransactions() {
    }

    static List<Transaction> withIds(JdbcClient jdbc, String... transactionIds) {
        return jdbc.sql("""
                        SELECT transaction_id, order_id, account_id, symbol, side, price, quantity, execution_time
                        FROM transactions
                        WHERE transaction_id IN (:ids)
                        """)
                .param("ids", List.of(transactionIds))
                .query((row, rowNumber) -> new Transaction(
                        row.getString("transaction_id"),
                        row.getString("order_id"),
                        row.getString("account_id"),
                        row.getString("symbol"),
                        Side.valueOf(row.getString("side")),
                        row.getBigDecimal("price"),
                        row.getBigDecimal("quantity"),
                        row.getObject("execution_time", OffsetDateTime.class).toInstant()))
                .list();
    }
}
