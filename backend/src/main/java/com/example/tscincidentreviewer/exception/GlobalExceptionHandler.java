package com.example.tscincidentreviewer.exception;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MissingFileException.class)
  public ResponseEntity<Map<String, Object>> handleMissingFile(MissingFileException ex) {
    log.warn("Bad request: file is required");

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("message", "file is required");
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(MissingHeadersException.class)
  public ResponseEntity<Map<String, Object>> handleMissingHeaders(MissingHeadersException ex) {
    String message = safeMessage(ex.getMessage(), "missing required headers");
    List<String> missingHeaders = ex.getMissingHeaders() == null ? List.of() : ex.getMissingHeaders();

    log.warn("Bad request: {}", message);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("message", message);
    body.put("missingHeaders", missingHeaders);
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(InvalidXlsxException.class)
  public ResponseEntity<Map<String, Object>> handleInvalidXlsx(InvalidXlsxException ex) {
    log.warn("Bad request: invalid xlsx");

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("message", "invalid xlsx");

    Throwable cause = ex.getCause();
    if (cause != null) {
      String causeMessage = cause.getMessage();
      if (causeMessage != null && !causeMessage.isBlank()) {
        body.put("details", causeMessage);
      }
    }

    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
    String message = safeMessage(ex.getMessage(), "bad request");
    log.warn("Bad request: {}", message);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("message", message);
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
    log.error("Unhandled server exception", ex);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("message", "internal server error");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  private String safeMessage(String message, String fallback) {
    if (message == null || message.isBlank()) {
      return fallback;
    }
    return message;
  }
}
