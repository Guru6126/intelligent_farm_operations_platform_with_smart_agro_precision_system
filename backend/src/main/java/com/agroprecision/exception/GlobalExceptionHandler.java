package com.agroprecision.exception;

import com.agroprecision.dto.AuthDtos.ApiMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiMessage> api(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(new ApiMessage(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiMessage> invalid(MethodArgumentNotValidException e) {
        var f = e.getBindingResult().getFieldError();
        String msg = f == null ? "Invalid input" : f.getField() + ": " + f.getDefaultMessage();
        return ResponseEntity.badRequest().body(new ApiMessage(msg));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiMessage> integrity(DataIntegrityViolationException e) {
        String m = e.getMostSpecificCause().getMessage();
        String msg = m != null && m.contains("Duplicate") ? "That record already exists." : "Invalid data: please check required fields and linked records.";
        if (m != null && (m.contains("uq_farm_plot_name") || (m.contains("Duplicate") && m.toLowerCase().contains("plot")))) {
            msg = "A plot with this name already exists on this farm. Duplication of plots is not allowed.";
        }
        return ResponseEntity.badRequest().body(new ApiMessage(msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiMessage> other(Exception e) {
        log.error("Unhandled error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiMessage("Something went wrong on the server."));
    }
}
