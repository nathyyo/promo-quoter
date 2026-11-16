package com.promoquoter.idempotency;

import com.promoquoter.common.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * The key was already used for a different request.
 */
public class IdempotencyKeyReusedException extends ApiException {

    public IdempotencyKeyReusedException(String key) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_REUSED", "Idempotency key reused",
                "Idempotency-Key '" + key + "' was already used for a different request.");
    }
}
