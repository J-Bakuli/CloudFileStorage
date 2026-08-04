package com.jb.cloudstorage.cloud_storage.exception;

import com.jb.cloudstorage.cloud_storage.dto.ApiErrorResponse;
import com.jb.cloudstorage.cloud_storage.dto.ApiFieldError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentialsException(InvalidCredentialsException ex, HttpServletRequest request) {
        log.debug("Unauthorized request: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        return jsonError(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized request",
                request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        log.debug("Constraint violation request: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        return jsonError(
                HttpStatus.BAD_REQUEST,
                "Invalid path",
                request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.debug("Bad request: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        return jsonError(
                HttpStatus.BAD_REQUEST,
                "Invalid request body",
                request.getRequestURI(),
                List.of());
    }
    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUsernameAlreadyExistsException(UsernameAlreadyExistsException ex, HttpServletRequest request) {
        log.debug("Username conflict: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        return jsonError(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex, HttpServletRequest request) {
        log.debug("Resource conflict: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        return jsonError(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.debug("Resource not found: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        return jsonError(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequestException(InvalidRequestException ex, HttpServletRequest request) {
        log.debug("Invalid request: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        return jsonError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiErrorResponse> handleStorageException(StorageException ex, HttpServletRequest request) {
        log.error("Storage exception: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        return jsonError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Storage error",
                request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiFieldError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(err -> List.of(new ApiFieldError(err.getField(), err.getDefaultMessage())))
                .orElse(List.of());
        if (!errors.isEmpty()) {
            ApiFieldError fieldError = errors.get(0);
            log.debug("Validation failed: uri={}, field={}, message={}",
                    request.getRequestURI(), fieldError.field(), fieldError.message());
        } else {
            log.debug("Validation failed: uri={}", request.getRequestURI());
        }
        return jsonError(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI(),
                errors
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: uri={}", request.getRequestURI(), ex);
        return jsonError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                request.getRequestURI(),
                List.of());
    }

    private ResponseEntity<ApiErrorResponse> jsonError(
            HttpStatus status,
            String message,
            String path,
            List<ApiFieldError> errors
    ) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildError(status, message, path, errors));
    }

    private ApiErrorResponse buildError(
            HttpStatus status,
            String message,
            String path,
            List<ApiFieldError> errors
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                errors
        );
    }
}
