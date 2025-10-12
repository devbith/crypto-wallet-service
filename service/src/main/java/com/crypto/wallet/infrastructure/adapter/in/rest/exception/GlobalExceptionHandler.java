package com.crypto.wallet.infrastructure.adapter.in.rest.exception;

import com.crypto.wallet.infrastructure.adapter.in.rest.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<String> errors = ex.getBindingResult().getFieldErrors().stream().map(this::formatFieldError).collect(Collectors.toList());

    logger.warn("Validation error on {}: {}", request.getRequestURI(), errors);

    var errorResponse = ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Validation failed", errors, request.getRequestURI());

    return ResponseEntity.badRequest().body(errorResponse);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
    logger.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
    var errorResponse = ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred", request.getRequestURI());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  private String formatFieldError(FieldError error) {
    return String.format("%s: %s", error.getField(), error.getDefaultMessage());
  }
}
