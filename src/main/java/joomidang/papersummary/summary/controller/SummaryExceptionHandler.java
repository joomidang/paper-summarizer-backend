package joomidang.papersummary.summary.controller;

import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.paper.exception.AccessDeniedException;
import joomidang.papersummary.summary.exception.SummaryCreationFailedException;
import joomidang.papersummary.summary.exception.SummaryNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SummaryExceptionHandler {

    @ExceptionHandler(SummaryNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSummaryNotFoundException(SummaryNotFoundException ex) {
        log.error("Summary not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(SummaryCreationFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleSummaryCreationFailedException(SummaryCreationFailedException ex) {
        log.error("Summary creation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }
}