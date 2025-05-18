package org.hackit.auth.exception;

import static org.springframework.http.HttpStatus.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hackit.auth.dto.ApiErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException ex,
            @NonNull final HttpHeaders headers,
            @NonNull final HttpStatusCode status,
            @NonNull final WebRequest request) {
        final Map<String, List<String>> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(error.getField(), key -> new ArrayList<>())
                    .add(error.getDefaultMessage());
        }

        final var problemDetail =
                ProblemDetailBuilder.forStatusAndDetail(status, "Request validation failed")
                        .withErrorType(ErrorType.REQUEST_VALIDATION_FAILED)
                        .withProperty("errors", errors)
                        .build();

        // Also add support for ApiErrorResponse
        if (request instanceof ServletWebRequest servletRequest) {
            String path = servletRequest.getRequest().getRequestURI();
            ApiErrorResponse apiError =
                    ApiErrorResponse.validationError("Validation failed", path, errors);
            problemDetail.setProperty("apiErrorResponse", apiError);
        }

        return new ResponseEntity<>(problemDetail, status);
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ProblemDetail> handleMissingRequestCookieException(
            final MissingRequestCookieException ex, HttpServletRequest request) {
        final var problemDetail =
                ProblemDetailBuilder.forStatusAndDetail(BAD_REQUEST, "Required cookie is missing")
                        .withErrorType(ErrorType.REQUEST_VALIDATION_FAILED)
                        .withProperty(
                                "apiErrorResponse",
                                ApiErrorResponse.of(
                                        "MISSING_COOKIE", ex.getMessage(), request.getRequestURI()))
                        .build();

        return new ResponseEntity<>(problemDetail, BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatchException(
            final MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "Parameter [%s] contains an invalid value".formatted(ex.getName());
        final var problemDetail =
                ProblemDetailBuilder.forStatusAndDetail(BAD_REQUEST, message)
                        .withErrorType(ErrorType.REQUEST_VALIDATION_FAILED)
                        .withProperty(
                                "apiErrorResponse",
                                ApiErrorResponse.of(
                                        "INVALID_PARAMETER", message, request.getRequestURI()))
                        .build();

        return new ResponseEntity<>(problemDetail, BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(
            final AuthenticationException ex, HttpServletRequest request) {
        final var problemDetail =
                ProblemDetailBuilder.forStatusAndDetail(UNAUTHORIZED, ex.getMessage())
                        .withErrorType(ErrorType.UNAUTHORIZED)
                        .withProperty(
                                "apiErrorResponse",
                                ApiErrorResponse.of(
                                        "UNAUTHORIZED", ex.getMessage(), request.getRequestURI()))
                        .build();

        if (log.isDebugEnabled()) {
            log.debug("Authorization exception stack trace: ", ex);
        }

        return new ResponseEntity<>(problemDetail, UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(
            final AccessDeniedException ex, HttpServletRequest request) {
        final var problemDetail =
                ProblemDetailBuilder.forStatusAndDetail(FORBIDDEN, "Access denied")
                        .withErrorType(ErrorType.FORBIDDEN)
                        .withProperty(
                                "apiErrorResponse",
                                ApiErrorResponse.of(
                                        "FORBIDDEN", "Access denied", request.getRequestURI()))
                        .build();

        return new ResponseEntity<>(problemDetail, FORBIDDEN);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(
            final ConstraintViolationException ex, @NonNull final WebRequest webRequest) {
        String path = "";
        if (webRequest instanceof ServletWebRequest servletRequest) {
            path = servletRequest.getRequest().getRequestURI();
        }

        final var problemDetail =
                ProblemDetailBuilder.forStatusAndDetail(CONFLICT, "Entity constraint violation")
                        .withErrorType(ErrorType.RESOURCE_ALREADY_EXISTS)
                        .withProperty(
                                "apiErrorResponse",
                                ApiErrorResponse.of("CONSTRAINT_VIOLATION", ex.getMessage(), path))
                        .build();

        return new ResponseEntity<>(problemDetail, CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            final Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred", ex);

        final var problemDetail =
                ProblemDetailBuilder.forStatusAndDetail(
                                INTERNAL_SERVER_ERROR, "An unexpected error occurred")
                        .withErrorType(ErrorType.UNKNOWN_SERVER_ERROR)
                        .withProperty(
                                "apiErrorResponse",
                                ApiErrorResponse.of(
                                        "INTERNAL_ERROR",
                                        "An unexpected error occurred",
                                        request.getRequestURI()))
                        .build();

        return new ResponseEntity<>(problemDetail, INTERNAL_SERVER_ERROR);
    }
}
