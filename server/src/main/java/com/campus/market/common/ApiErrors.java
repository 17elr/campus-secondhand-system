package com.campus.market.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<?> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(new ApiResponse<>(e.getStatusCode().value(), e.getReason(), null));
  }

  @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
  ResponseEntity<?> bad(Exception e) {
    return ResponseEntity.badRequest()
        .body(
            new ApiResponse<>(
                400, e instanceof IllegalArgumentException ? e.getMessage() : "字段格式或长度不正确", null));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<?> conflict() {
    return ResponseEntity.status(409).body(new ApiResponse<>(409, "数据冲突，请刷新后重试", null));
  }
}
