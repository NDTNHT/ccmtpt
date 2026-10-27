package com.taskmanagement.adapter.api;

import com.taskmanagement.adapter.api.dto.ApiResponse;
import com.taskmanagement.application.exception.ProjectNotFoundException;
import com.taskmanagement.application.exception.TaskNotFoundException;
import com.taskmanagement.domain.common.exception.DomainException;
import com.taskmanagement.domain.task.exception.InvalidTaskStateTransitionException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Maps service and domain exceptions to the API error envelope.
 */
@RestControllerAdvice
public class GlobalApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

    @ExceptionHandler({TaskNotFoundException.class, ProjectNotFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(RuntimeException exception) {
        return failure(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvalidTaskStateTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleStateConflict(
        InvalidTaskStateTransitionException exception
    ) {
        return failure(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", exception.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainError(DomainException exception) {
        return failure(HttpStatus.UNPROCESSABLE_ENTITY, "DOMAIN_VALIDATION_ERROR", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String details = exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(FieldError::getDefaultMessage)
            .orElse("Request validation failed");
        return failure(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return failure(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", exception.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(Exception exception) {
        return failure(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
            "Request body, path parameter, or query parameter is invalid");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedError(Exception exception) {
        logger.error("Unexpected error while handling API request", exception);
        return failure(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
            "An unexpected server error occurred");
    }

    private ResponseEntity<ApiResponse<Void>> failure(HttpStatus status, String code, String details) {
        return ResponseEntity.status(status)
            .body(ApiResponse.failure(status.getReasonPhrase(), code, details));
    }
}
