package com.jb.cloudstorage.cloud_storage.exception;

import com.jb.cloudstorage.cloud_storage.dto.ApiErrorResponse;
import com.jb.cloudstorage.cloud_storage.dto.ApiFieldError;
import io.minio.errors.ErrorResponseException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleInvalidCredentialsException(InvalidCredentialsException ex, HttpServletRequest request) {
        log.warn("Unauthorized request: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        return buildError(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleUsernameAlreadyExistsException(UsernameAlreadyExistsException ex, HttpServletRequest request) {
        log.debug("Username conflict: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        return buildError(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(DirectoryAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleDirectoryAlreadyExistsException(DirectoryAlreadyExistsException ex, HttpServletRequest request) {
        log.debug("Directory conflict: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        return buildError(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(FileAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleFileAlreadyExistsException(FileAlreadyExistsException ex, HttpServletRequest request) {
        log.debug("File conflict: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        return buildError(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.debug("Resource not found: uri={}, message={}", request.getRequestURI(), ex.getMessage());
        return buildError(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
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
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI(),
                errors
        );
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleErrorResponseException(ErrorResponseException ex, HttpServletRequest request) {
        if ("NoSuchKey".equals(ex.errorResponse().code())) {
            log.debug("MinIO object not found: uri={}", request.getRequestURI());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildError(
                    HttpStatus.NOT_FOUND,
                    "Resource not found",
                    request.getRequestURI(),
                    List.of()));
        }
        log.error("MinIO error: uri={}, code={}", request.getRequestURI(), ex.errorResponse().code(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Storage error",
                request.getRequestURI(),
                List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: uri={}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                request.getRequestURI(),
                List.of()));
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
