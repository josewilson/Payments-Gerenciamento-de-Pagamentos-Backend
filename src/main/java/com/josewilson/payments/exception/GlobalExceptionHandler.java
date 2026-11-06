package com.josewilson.payments.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = body(HttpStatus.BAD_REQUEST, "Dados inválidos.");
        body.put("validationErrors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler({MissingRequestHeaderException.class, HandlerMethodValidationException.class})
    public ResponseEntity<Map<String, Object>> handleBadHeader(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Header Idempotency-Key é obrigatório (1 a 100 caracteres).");
    }

    @ExceptionHandler({InvalidStatusTransitionException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(Exception ex) {
        String message = ex instanceof InvalidStatusTransitionException
                ? ex.getMessage()
                : "Pagamento alterado por outra requisição. Tente novamente.";
        return build(HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler(CotacaoIndisponivelException.class)
    public ResponseEntity<Map<String, Object>> handleCotacaoIndisponivel(CotacaoIndisponivelException ex) {
        log.warn("Cotação indisponível: {}", ex.getMessage(), ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Cotação indisponível no momento. Tente novamente.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(Exception ex) {
        log.error("Erro inesperado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno.");
    }

    private static ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(body(status, message));
    }

    private static Map<String, Object> body(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }
}
