package io.github.tomaszfranczak.batchprocessor.transaction;

record StoreResult(int stored, int skippedDuplicates) {
}
