package com.careersphere.exception;

import com.careersphere.dto.Dto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Dto.ApiError> handleApi(ApiException exception, HttpServletRequest request) {
        return error(exception.getStatus(), exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Dto.ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
            fields.putIfAbsent(error.getField(), error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Please check the highlighted fields.", fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Dto.ApiError> handleConstraint(ConstraintViolationException exception) {
        return error(HttpStatus.BAD_REQUEST, "The request contains an invalid value.", Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Dto.ApiError> handleUploadLimit(MaxUploadSizeExceededException exception) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "This PDF is larger than the 8 MB upload limit.", Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Dto.ApiError> handleMalformed(HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "The request body is invalid or incomplete.", Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Dto.ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "This HTTP method is not supported for this endpoint.", Map.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<Dto.ApiError> handleMediaType(HttpMediaTypeNotSupportedException exception) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Use a supported content type for this endpoint.", Map.of());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    ResponseEntity<Dto.ApiError> handleMissingParameter(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "A required request field or file is missing.", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Dto.ApiError> handleConflict(DataIntegrityViolationException exception) {
        return error(HttpStatus.CONFLICT, "This change conflicts with an existing record.", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Dto.ApiError> handleUnexpected(Exception exception) {
        log.error("CareerSphere request failed unexpectedly", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.", Map.of());
    }

    private ResponseEntity<Dto.ApiError> error(HttpStatus status, String message, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new Dto.ApiError(status.value(), message, LocalDateTime.now(), fields));
    }
}
