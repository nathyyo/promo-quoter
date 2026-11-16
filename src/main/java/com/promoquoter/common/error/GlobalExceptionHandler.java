package com.promoquoter.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns every error into an RFC 7807 {@link ProblemDetail}. Each body carries a stable {@code code}
 * property for clients, and validation errors also list the offending fields under {@code errors}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String VALIDATION_FAILED = "VALIDATION_FAILED";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApiException(ApiException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setTitle(ex.getTitle());
        problem.setProperty("code", ex.getCode());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setTitle("Internal server error");
        problem.setProperty("code", "INTERNAL_ERROR");
        return problem;
    }

    /** {@code @Valid} on a request body object. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> violations.add(new FieldViolation(error.getField(), error.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> violations.add(new FieldViolation(error.getObjectName(), error.getDefaultMessage())));
        return validationProblem(violations, headers, request, ex);
    }

    /** Constraints on method parameters, including elements of a list body such as {@code List<@Valid T>}. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = new ArrayList<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String prefix = parameterPath(result);
            if (result instanceof ParameterErrors errors) {
                for (FieldError error : errors.getFieldErrors()) {
                    violations.add(new FieldViolation(join(prefix, error.getField()), error.getDefaultMessage()));
                }
                errors.getGlobalErrors()
                        .forEach(error -> violations.add(new FieldViolation(prefix, error.getDefaultMessage())));
            } else {
                for (MessageSourceResolvable error : result.getResolvableErrors()) {
                    violations.add(new FieldViolation(prefix, error.getDefaultMessage()));
                }
            }
        }
        return validationProblem(violations, headers, request, ex);
    }

    private ResponseEntity<Object> validationProblem(
            List<FieldViolation> violations, HttpHeaders headers, WebRequest request, Exception ex) {
        violations.sort(Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::message));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "The request contains " + violations.size() + " invalid value(s).");
        problem.setTitle("Validation failed");
        problem.setProperty("code", VALIDATION_FAILED);
        problem.setProperty("errors", violations);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    private static String parameterPath(ParameterValidationResult result) {
        String base = result.getMethodParameter().hasParameterAnnotation(RequestBody.class)
                ? ""
                : result.getMethodParameter().getParameterName();
        if (result.getContainerIndex() != null) {
            return (base == null ? "" : base) + "[" + result.getContainerIndex() + "]";
        }
        if (result.getContainerKey() != null) {
            return (base == null ? "" : base) + "[" + result.getContainerKey() + "]";
        }
        return base == null || base.isEmpty() ? "body" : base;
    }

    private static String join(String prefix, String field) {
        if (prefix.isEmpty()) {
            return field;
        }
        return prefix + "." + field;
    }
}
