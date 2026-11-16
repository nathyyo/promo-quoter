package com.promoquoter.idempotency;

/**
 * The outcome of an idempotent operation.
 *
 * @param replayed {@code true} if the value is the stored result of an earlier request with the same key
 */
public record IdempotentResult<T>(T value, boolean replayed) {
}
