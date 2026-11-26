package io.github.tomaszfranczak.batchprocessor.transaction;

class InvalidTransactionException extends RuntimeException {

    InvalidTransactionException(String message) {
        super(message);
    }
}
