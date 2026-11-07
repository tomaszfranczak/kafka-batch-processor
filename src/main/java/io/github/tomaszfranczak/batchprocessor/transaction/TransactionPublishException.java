package io.github.tomaszfranczak.batchprocessor.transaction;

public class TransactionPublishException extends RuntimeException {

    public TransactionPublishException(String transactionId, Throwable cause) {
        super("Transaction " + transactionId + " could not be published", cause);
    }
}
