package com.promoquoter.idempotency;

import com.promoquoter.common.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Another request with the same key is still being processed.
 */
public class IdempotencyKeyInProgressException extends ApiException {

    public IdempotencyKeyInProgressException(String key) {
        super(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_IN_PROGRESS", "Request in progress",
                "A request with Idempotency-Key '" + key + "' is still being processed. Please retry.");
    }
}
