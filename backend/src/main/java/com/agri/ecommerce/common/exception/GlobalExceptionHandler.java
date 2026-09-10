package com.agri.ecommerce.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ApiError> handleApplicationException(
        ApplicationException exception,
        HttpServletRequest request
    ) {
        return buildError(
            exception.getStatus(),
            exception.getCode(),
            exception.getMessage(),
            request,
            List.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        List<FieldValidationError> fieldErrors = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::toFieldValidationError)
            .toList();

        return buildError(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "Du lieu gui len khong hop le",
            request,
            fieldErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraintViolation(
        ConstraintViolationException exception,
        HttpServletRequest request
    ) {
        List<FieldValidationError> fieldErrors = exception.getConstraintViolations()
            .stream()
            .map(violation -> new FieldValidationError(
                violation.getPropertyPath().toString(),
                violation.getMessage()
            ))
            .toList();

        return buildError(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "Tham so khong hop le",
            request,
            fieldErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableBody(
        HttpMessageNotReadableException exception,
        HttpServletRequest request
    ) {
        return buildError(
            HttpStatus.BAD_REQUEST,
            "MALFORMED_REQUEST",
            "Noi dung yeu cau khong dung dinh dang",
            request,
            List.of()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleDataConflict(
        DataIntegrityViolationException exception,
        HttpServletRequest request
    ) {
        return buildError(
            HttpStatus.CONFLICT,
            "DATA_CONFLICT",
            "Du lieu da ton tai hoac dang duoc su dung",
            request,
            List.of()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleNotFound(
        NoResourceFoundException exception,
        HttpServletRequest request
    ) {
        return buildError(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            "Khong tim thay tai nguyen",
            request,
            List.of()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> handleIllegalArgument(
        IllegalArgumentException exception,
        HttpServletRequest request
    ) {
        return buildError(
            HttpStatus.BAD_REQUEST,
            "INVALID_ARGUMENT",
            exception.getMessage(),
            request,
            List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(
        Exception exception,
        HttpServletRequest request
    ) {
        log.error("Unhandled error at {}", request.getRequestURI(), exception);
        return buildError(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "He thong dang gap loi, vui long thu lai sau",
            request,
            List.of()
        );
    }

    private FieldValidationError toFieldValidationError(FieldError error) {
        return new FieldValidationError(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ApiError> buildError(
        HttpStatus status,
        String code,
        String message,
        HttpServletRequest request,
        List<FieldValidationError> fieldErrors
    ) {
        ApiError body = new ApiError(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            code,
            message,
            request.getRequestURI(),
            fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
