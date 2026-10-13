package com.remosens.geospatials.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.remosens.geospatials.dto.ApiError;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DuplicateRegistrationException.class)
    public ResponseEntity<ApiError> handleDuplicateRegistration(
            DuplicateRegistrationException ex, WebRequest request) {
        return response(HttpStatus.CONFLICT, ex.getMessage(), request,
                Map.of("email", "This email is already registered for this role."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Database constraint violation on request path {}", path(request));
        return response(HttpStatus.CONFLICT,
                "The request conflicts with existing data. Check the submitted values and try again.",
                request, Map.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String field = violation.getPropertyPath().toString();
            int dot = field.lastIndexOf('.');
            field = dot >= 0 ? field.substring(dot + 1) : field;
            fieldErrors.putIfAbsent(field, violation.getMessage());
        });
        return response(HttpStatus.BAD_REQUEST, "One or more values are invalid.", request, fieldErrors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unhandled exception for request path {}", path(request), ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.", request, Map.of());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(topLevelField(error.getField()), error.getDefaultMessage()));
        ApiError body = ApiError.of(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "One or more fields are invalid. Correct them and try again.",
                path(request), fieldErrors);
        return new ResponseEntity<>(body, headers, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String message = "The request body is missing or contains invalid JSON.";
        Map<String, String> fieldErrors = Map.of();

        InvalidFormatException invalidFormat = findInvalidFormat(ex);
        if (invalidFormat != null) {
            String field = invalidFormat.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(name -> name != null && !name.isBlank())
                    .findFirst().orElse("value");
            message = "A supplied value is invalid for field '" + field + "'.";
            fieldErrors = Map.of(field, "Provide a valid value. For enum fields, use one of the supported names.");
        }

        ApiError body = ApiError.of(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), message, path(request), fieldErrors);
        return new ResponseEntity<>(body, headers, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        int status = statusCode.value();
        HttpStatus resolved = HttpStatus.resolve(status);
        String error = resolved != null ? resolved.getReasonPhrase() : "HTTP Error";
        ApiError apiError = ApiError.of(status, error, friendlyMessage(status), path(request), Map.of());
        return new ResponseEntity<>(apiError, headers, statusCode);
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status, String message, WebRequest request, Map<String, String> fieldErrors) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), message, path(request), fieldErrors));
    }

    private static String path(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI() : "";
    }

    private static String topLevelField(String field) {
        int bracket = field.indexOf('[');
        int dot = field.indexOf('.');
        int end = field.length();
        if (bracket >= 0) end = Math.min(end, bracket);
        if (dot >= 0) end = Math.min(end, dot);
        return field.substring(0, end);
    }

    private static InvalidFormatException findInvalidFormat(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof InvalidFormatException invalidFormat) return invalidFormat;
            current = current.getCause();
        }
        return null;
    }

    private static String friendlyMessage(int status) {
        return switch (status) {
            case 400 -> "The request is invalid. Check the request body and field values.";
            case 404 -> "The requested resource was not found.";
            case 405 -> "This HTTP method is not supported for this endpoint.";
            case 406 -> "The requested response format is not supported.";
            case 415 -> "The request content type is not supported. Send JSON with Content-Type: application/json.";
            case 429 -> "Too many requests. Please try again later.";
            default -> status >= 500
                    ? "The server could not complete the request. Please try again later."
                    : "The request could not be completed.";
        };
    }
}
