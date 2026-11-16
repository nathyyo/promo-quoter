package com.promoquoter.common.error;

import org.springframework.http.HttpStatus;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * The request is well-formed but refers to resources that do not exist.
 */
public class InvalidReferenceException extends ApiException {

    public InvalidReferenceException(String resource, Collection<?> missingIds) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "UNKNOWN_" + resource.toUpperCase(), "Unknown " + resource.toLowerCase(),
                resource + " not found: " + missingIds.stream().map(String::valueOf).sorted()
                        .collect(Collectors.joining(", ")) + ".");
    }
}
