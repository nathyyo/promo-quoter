package com.promoquoter.common.error;

/**
 * One invalid input value, reported in the {@code errors} array of a validation problem response.
 *
 * @param field   path to the value, e.g. {@code items[0].qty}
 * @param message why the value was rejected
 */
public record FieldViolation(String field, String message) {
}
