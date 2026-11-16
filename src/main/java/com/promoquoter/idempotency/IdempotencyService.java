package com.promoquoter.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Runs an operation at most once per {@code Idempotency-Key}.
 *
 * <ul>
 *   <li>First request: the key is claimed and the operation runs in the same transaction. The key
 *       and the operation's result are committed together, or not at all.</li>
 *   <li>Repeat with the same request: the stored result is returned and the operation does not run.</li>
 *   <li>Repeat with a different request: rejected with 422.</li>
 *   <li>Concurrent requests with the same key: the database primary key lets only one claim it; the
 *       others wait for it, then receive its stored result.</li>
 * </ul>
 *
 * Only successful results are stored. If the operation fails (for example, out of stock), the key is
 * released and the client may retry with it.
 */
@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository records;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transaction;
    private final TransactionTemplate readOnlyTransaction;

    public IdempotencyService(IdempotencyRecordRepository records, ObjectMapper objectMapper,
                              PlatformTransactionManager transactionManager) {
        this.records = records;
        this.objectMapper = objectMapper;
        this.transaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
    }

    /**
     * @param key         the client's idempotency key
     * @param fingerprint identifies the request; reusing the key with another fingerprint is an error
     * @param resultType  type used to read a stored result back
     * @param operation   the work to run once; it joins the transaction that claims the key
     */
    public <T> IdempotentResult<T> execute(String key, String fingerprint, Class<T> resultType, Supplier<T> operation) {
        Optional<T> stored = findStoredResult(key, fingerprint, resultType);
        if (stored.isPresent()) {
            return new IdempotentResult<>(stored.get(), true);
        }
        try {
            T result = transaction.execute(status -> {
                IdempotencyRecord record = claim(key, fingerprint);
                T value = operation.get();
                record.complete(write(value));
                return value;
            });
            return new IdempotentResult<>(result, false);
        } catch (KeyAlreadyClaimedException claimedConcurrently) {
            // Another request claimed the key first. We waited on its row lock, so it has normally
            // committed by now; if it is still running or failed, there is nothing to replay yet.
            return findStoredResult(key, fingerprint, resultType)
                    .map(value -> new IdempotentResult<>(value, true))
                    .orElseThrow(() -> new IdempotencyKeyInProgressException(key));
        }
    }

    /** Inserts the key. Only failures of this insert mean another request holds the key. */
    private IdempotencyRecord claim(String key, String fingerprint) {
        try {
            return records.saveAndFlush(IdempotencyRecord.claim(key, fingerprint));
        } catch (DataIntegrityViolationException | PessimisticLockingFailureException e) {
            throw new KeyAlreadyClaimedException(e);
        }
    }

    private static final class KeyAlreadyClaimedException extends RuntimeException {
        KeyAlreadyClaimedException(Throwable cause) {
            super(cause);
        }
    }

    private <T> Optional<T> findStoredResult(String key, String fingerprint, Class<T> resultType) {
        Optional<IdempotencyRecord> found = readOnlyTransaction.execute(status -> records.findById(key));
        if (found == null || found.isEmpty()) {
            return Optional.empty();
        }
        IdempotencyRecord record = found.get();
        if (!record.matches(fingerprint)) {
            throw new IdempotencyKeyReusedException(key);
        }
        if (!record.isComplete()) {
            throw new IdempotencyKeyInProgressException(key);
        }
        return Optional.of(read(record.getResponseBody(), resultType));
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not store idempotent result", e);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not read stored idempotent result", e);
        }
    }
}
