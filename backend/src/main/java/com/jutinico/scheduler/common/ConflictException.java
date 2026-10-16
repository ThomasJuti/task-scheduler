package com.jutinico.scheduler.common;

import org.springframework.http.HttpStatus;

/** The request is valid but conflicts with the current state (e.g. task already assigned). */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
