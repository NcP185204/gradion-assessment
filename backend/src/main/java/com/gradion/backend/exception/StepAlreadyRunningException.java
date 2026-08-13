package com.gradion.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // 409 Conflict
public class StepAlreadyRunningException extends RuntimeException {
    public StepAlreadyRunningException(String message) {
        super(message);
    }
}
