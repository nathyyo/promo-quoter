package com.promoquoter.common.error;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unexpectedErrorsReturn500WithoutLeakingInternals() {
        ProblemDetail problem = handler.handleUnexpected(new IllegalStateException("db password is hunter2"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred.").doesNotContain("hunter2");
        assertThat(problem.getProperties()).containsEntry("code", "INTERNAL_ERROR");
    }

    @Test
    void lockTimeoutsAndVersionConflictsReturnARetryable409() {
        ProblemDetail pessimistic = handler.handleConcurrencyFailure(new PessimisticLockingFailureException("timeout"));
        ProblemDetail optimistic = handler.handleConcurrencyFailure(new OptimisticLockingFailureException("stale"));

        assertThat(pessimistic.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(pessimistic.getProperties()).containsEntry("code", "CONCURRENT_UPDATE");
        assertThat(pessimistic.getDetail()).contains("Please retry");
        assertThat(optimistic.getProperties()).containsEntry("code", "CONCURRENT_UPDATE");
    }

    @Test
    void apiExceptionsCarryTheirStatusCodeAndExtraProperties() {
        ApiException ex = new ResourceNotFoundException("Product", "42");

        ProblemDetail problem = handler.handleApiException(ex);

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getTitle()).isEqualTo("Product not found");
        assertThat(problem.getProperties()).containsEntry("code", "RESOURCE_NOT_FOUND");
    }
}
