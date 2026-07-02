package com.jb.cloudstorage.cloud_storage.exception;

import com.jb.cloudstorage.cloud_storage.dto.ApiErrorResponse;
import com.jb.cloudstorage.cloud_storage.dto.ApiFieldError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleInvalidCredentialsException(InvalidCredentialsException ex, HttpServletRequest request) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request.getPathInfo(),
                List.of());
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleUsernameAlreadyExistsException(UsernameAlreadyExistsException ex, HttpServletRequest request) {
        return buildError(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getPathInfo(),
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
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI(),
                errors
        );
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
