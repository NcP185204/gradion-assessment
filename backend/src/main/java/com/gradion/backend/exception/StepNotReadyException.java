package com.gradion.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST) // 400 Bad Request
public class StepNotReadyException extends RuntimeException {
    public StepNotReadyException(String message) {
        super(message);
    }
}
