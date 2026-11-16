package com.promoquoter.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.Objects;

/**
 * Remembers the outcome of a request made with an {@code Idempotency-Key}. The key is the primary
 * key, so the database guarantees that only one request can claim it.
 *
 * <p>Implements {@link Persistable} so that saving a new record always issues an INSERT. With an
 * assigned id, Spring Data would otherwise merge and could overwrite a key claimed by another request.
 */
@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord implements Persistable<String> {

    @Id
    @Column(name = "idempotency_key", length = 255)
    private String key;

    /** Fingerprint of the request that claimed the key; a different request may not reuse it. */
    @Column(nullable = false, length = 64)
    private String requestFingerprint;

    @Lob
    @Column(name = "response_body")
    private String responseBody;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    protected IdempotencyRecord() {
        // for JPA
    }

    private IdempotencyRecord(String key, String requestFingerprint) {
        this.key = Objects.requireNonNull(key, "key");
        this.requestFingerprint = Objects.requireNonNull(requestFingerprint, "requestFingerprint");
        this.createdAt = Instant.now();
    }

    /** Claims a key for a request that has not completed yet. */
    public static IdempotencyRecord claim(String key, String requestFingerprint) {
        return new IdempotencyRecord(key, requestFingerprint);
    }

    public void complete(String responseBody) {
        this.responseBody = Objects.requireNonNull(responseBody, "responseBody");
    }

    public boolean matches(String fingerprint) {
        return requestFingerprint.equals(fingerprint);
    }

    public boolean isComplete() {
        return responseBody != null;
    }

    @Override
    public String getId() {
        return key;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
