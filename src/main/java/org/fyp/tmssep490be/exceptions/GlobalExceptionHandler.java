package org.fyp.tmssep490be.exceptions;

import jakarta.persistence.EntityNotFoundException;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResponseObject<Void>> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseObject.error(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseObject<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage(),
                        (existing, replacement) -> existing + "; " + replacement
                ));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseObject.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .build());
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ResponseObject<Void>> handleNullPointerException(NullPointerException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseObject.error("Null value encountered in the application"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseObject<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseObject.error(e.getMessage()));
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ResponseObject<Void>> handleCustomException(CustomException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseObject.error(e.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ResponseObject<Void>> handleEntityNotFoundException(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseObject.error(e.getMessage()));
    }
}
