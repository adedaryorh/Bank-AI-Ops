package com.bankops.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        problem.setDetail("One or more fields are invalid.");
        List<Map<String, String>> violations = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            violations.add(Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())));
        }
        problem.setProperty("violations", violations);
        return problem(problem, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> denied(AccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Access denied");
        problem.setDetail("Your role is not allowed to perform this operation.");
        return problem(problem, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> unauthenticated(AuthenticationException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Authentication required");
        problem.setDetail("A valid bearer token for this API is required.");
        return problem(problem, request);
    }

    @ExceptionHandler({jakarta.validation.ConstraintViolationException.class,
        org.springframework.web.method.annotation.HandlerMethodValidationException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.multipart.support.MissingServletRequestPartException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class})
    ResponseEntity<ProblemDetail> invalidInput(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
            "One or more request parameters are invalid.");
        problem.setTitle("Invalid request");
        return problem(problem, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> badRequest(IllegalArgumentException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request");
        problem.setDetail(ex.getMessage());
        return problem(problem, request);
    }

    @ExceptionHandler(com.bankops.common.WorkspaceException.class)
    ResponseEntity<ProblemDetail> workspace(com.bankops.common.WorkspaceException ex, HttpServletRequest request) {
        return problem(ProblemDetail.forStatusAndDetail(ex.status(),ex.getMessage()),request);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<ProblemDetail> uploadTooLarge(Exception ex,HttpServletRequest request) {
        return problem(ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,"Maximum upload size is 1 MiB."),request);
    }

    @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class,
        org.springframework.web.servlet.NoHandlerFoundException.class})
    ResponseEntity<ProblemDetail> missingRoute(Exception ex,HttpServletRequest request) {
        return problem(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,"Resource not found."),request);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> wrongMethod(Exception ex,HttpServletRequest request) {
        return problem(ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED,"HTTP method not supported."),request);
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> wrongMediaType(Exception ex,HttpServletRequest request) {
        // Using static factory method to initialize the class.
        return problem(ProblemDetail.forStatusAndDetail(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Request content type is not supported."),request
            );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception ex, HttpServletRequest request) {
        
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal error");
        problem.setDetail("The request could not be completed. Quote the correlation id when reporting it.");
        return problem(problem, request);
    }

    private ResponseEntity<ProblemDetail> problem(ProblemDetail problem, HttpServletRequest request) {
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId.toString());
        }
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }
}
