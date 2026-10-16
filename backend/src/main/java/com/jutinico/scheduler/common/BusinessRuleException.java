package com.jutinico.scheduler.common;

import org.springframework.http.HttpStatus;

/** The request breaks a business rule (e.g. Req3a: task skills must be a subset of worker skills). */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}
