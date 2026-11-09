package io.github.tomaszfranczak.batchprocessor.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

@Repository
@RequiredArgsConstructor
class TransactionRepository {

    private static final String INSERT = """
            INSERT INTO transactions
                (transaction_id, order_id, account_id, symbol, side, price, quantity, execution_time)
            VALUES
                (:transactionId, :orderId, :accountId, :symbol, :side, :price, :quantity, :executionTime)
            ON CONFLICT (transaction_id) DO NOTHING
            """;

    private final NamedParameterJdbcTemplate jdbc;

    int saveAll(List<Transaction> transactions) {
        int[] rowCounts = jdbc.batchUpdate(INSERT, transactions.stream()
                .map(TransactionRepository::parameters)
                .toArray(SqlParameterSource[]::new));
        return Arrays.stream(rowCounts).sum();
    }

    private static SqlParameterSource parameters(Transaction transaction) {
        return new MapSqlParameterSource()
                .addValue("transactionId", transaction.transactionId())
                .addValue("orderId", transaction.orderId())
                .addValue("accountId", transaction.accountId())
                .addValue("symbol", transaction.symbol())
                .addValue("side", transaction.side().name())
                .addValue("price", transaction.price())
                .addValue("quantity", transaction.quantity())
                .addValue("executionTime", transaction.executionTime().atOffset(ZoneOffset.UTC));
    }
}
