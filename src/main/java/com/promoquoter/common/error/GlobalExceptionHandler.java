package com.promoquoter.common.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
        ex.getProperties().forEach(problem::setProperty);
        return problem;
    }

    /**
     * A row lock could not be obtained in time, or an optimistic version check failed because another
     * request changed the same data. Retrying the request is safe.
     */
    @ExceptionHandler({PessimisticLockingFailureException.class, OptimisticLockingFailureException.class})
    ProblemDetail handleConcurrencyFailure(Exception ex) {
        log.warn("Concurrent update conflict: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The data was changed by another request. Please retry.");
        problem.setTitle("Concurrent update");
        problem.setProperty("code", "CONCURRENT_UPDATE");
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
                        .forEach(error -> violations.add(new FieldViolation(orBody(prefix), error.getDefaultMessage())));
            } else {
                for (MessageSourceResolvable error : result.getResolvableErrors()) {
                    violations.add(new FieldViolation(orBody(prefix), error.getDefaultMessage()));
                }
            }
        }
        return validationProblem(violations, headers, request, ex);
    }

    /** Malformed JSON, unknown properties, wrong value types or an unknown polymorphic {@code type}. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, describe(ex.getCause()));
        problem.setTitle("Malformed request");
        problem.setProperty("code", "MALFORMED_REQUEST");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    private static String describe(Throwable cause) {
        return switch (cause) {
            case InvalidTypeIdException e -> e.getTypeId() == null
                    ? "Missing 'type' property" + at(e) + "."
                    : "Unknown type '" + e.getTypeId() + "'" + at(e) + ".";
            case UnrecognizedPropertyException e -> "Unknown property '" + e.getPropertyName() + "'" + at(e) + ".";
            case InvalidFormatException e -> "Invalid value '" + e.getValue() + "'" + at(e) + allowedValues(e) + ".";
            case MismatchedInputException e -> "Unexpected value" + at(e) + ".";
            case JsonProcessingException e -> "Request body is not valid JSON.";
            case null, default -> "Request body is missing or unreadable.";
        };
    }

    private static String at(JsonMappingException e) {
        String path = e.getPath().stream()
                .map(ref -> ref.getFieldName() != null ? "." + ref.getFieldName() : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining());
        if (path.startsWith(".")) {
            path = path.substring(1);
        }
        return path.isEmpty() ? "" : " at '" + path + "'";
    }

    private static String allowedValues(InvalidFormatException e) {
        Class<?> target = e.getTargetType();
        if (target != null && target.isEnum()) {
            return "; expected one of " + Arrays.toString(target.getEnumConstants());
        }
        return "";
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
        RequestHeader header = result.getMethodParameter().getParameterAnnotation(RequestHeader.class);
        String base;
        if (result.getMethodParameter().hasParameterAnnotation(RequestBody.class)) {
            base = "";
        } else if (header != null && !header.name().isEmpty()) {
            base = header.name();
        } else {
            base = result.getMethodParameter().getParameterName();
        }
        if (base == null) {
            base = "";
        }
        if (result.getContainerIndex() != null) {
            return base + "[" + result.getContainerIndex() + "]";
        }
        if (result.getContainerKey() != null) {
            return base + "[" + result.getContainerKey() + "]";
        }
        return base;
    }

    /** Errors about the request body as a whole are reported against {@code body}. */
    private static String orBody(String path) {
        return path.isEmpty() ? "body" : path;
    }

    private static String join(String prefix, String field) {
        if (prefix.isEmpty()) {
            return field;
        }
        return prefix + "." + field;
    }
}
