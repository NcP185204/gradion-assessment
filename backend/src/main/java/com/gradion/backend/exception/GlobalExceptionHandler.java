package com.gradion.backend.exception;

import com.gradion.backend.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(StepAlreadyRunningException.class)
    public ResponseEntity<ErrorResponse> handleStepAlreadyRunningException(StepAlreadyRunningException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse("STEP_ALREADY_RUNNING", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(StepNotReadyException.class)
    public ResponseEntity<ErrorResponse> handleStepNotReadyException(StepNotReadyException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse("STEP_NOT_READY", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(StepNotStuckException.class)
    public ResponseEntity<ErrorResponse> handleStepNotStuckException(StepNotStuckException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse("STEP_NOT_STUCK", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse("RESOURCE_NOT_FOUND", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse("INTERNAL_SERVER_ERROR", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
