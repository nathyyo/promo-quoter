package com.promoquoter.common.error;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", resource + " not found",
                resource + " with id '" + id + "' does not exist.");
    }
}
