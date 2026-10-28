package com.kisandaily.exceptions;

import com.kisandaily.response.ApiError;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiErrorHandler {
  @ExceptionHandler(ApiException.class)
  ResponseEntity<ApiError> known(ApiException e) {
    return ResponseEntity.status(e.status).body(new ApiError(e.code, e.getMessage(), Map.of()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> invalid(MethodArgumentNotValidException e) {
    Map<String, String> fields = new LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
    return ResponseEntity.badRequest()
        .body(new ApiError("VALIDATION_ERROR", "कृपया भरी हुई जानकारी जांचें", fields));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiError> malformed() {
    return ResponseEntity.badRequest()
        .body(new ApiError("INVALID_REQUEST", "सही JSON request भेजें", Map.of()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiError> duplicate() {
    return ResponseEntity.status(409)
        .body(
            new ApiError(
                "MOBILE_ALREADY_REGISTERED", "यह मोबाइल नंबर पहले से registered है", Map.of()));
  }
}
