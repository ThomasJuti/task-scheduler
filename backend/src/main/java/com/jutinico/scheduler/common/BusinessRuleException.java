package com.jutinico.scheduler.common;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}
