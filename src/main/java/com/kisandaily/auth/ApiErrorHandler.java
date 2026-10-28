package com.kisandaily.auth;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestControllerAdvice
class ApiErrorHandler {
    @ExceptionHandler(ApiException.class) ResponseEntity<AuthDtos.Error> known(ApiException e) { return ResponseEntity.status(e.status).body(new AuthDtos.Error(e.code,e.getMessage(),Map.of())); }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<AuthDtos.Error> invalid(MethodArgumentNotValidException e) {
        Map<String,String> fields=new LinkedHashMap<>();e.getBindingResult().getFieldErrors().forEach(f->fields.putIfAbsent(f.getField(),f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new AuthDtos.Error("VALIDATION_ERROR","कृपया भरी हुई जानकारी जांचें",fields));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class) ResponseEntity<AuthDtos.Error> malformed() {return ResponseEntity.badRequest().body(new AuthDtos.Error("INVALID_REQUEST","सही JSON request भेजें",Map.of()));}
    @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<AuthDtos.Error> duplicate() {return ResponseEntity.status(409).body(new AuthDtos.Error("USER_ID_TAKEN","यह User ID उपलब्ध नहीं है",Map.of()));}
}
