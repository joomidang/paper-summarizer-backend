package joomidang.papersummary.common.controller;

import static joomidang.papersummary.common.exception.CommonErrorCode.INTERNAL_SERVER_ERROR;
import static org.springframework.http.ResponseEntity.internalServerError;

import joomidang.papersummary.common.controller.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class CommonExceptionHandler {
    @ExceptionHandler
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(final NoResourceFoundException ex) {
        log.warn("No resource found exception: {}", ex.getResourcePath());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<Void>> handleException(final Exception ex) {
        log.error("Exception: {}", ex.getMessage());
        return internalServerError().body(ApiResponse.fail(INTERNAL_SERVER_ERROR, ex.getMessage()));
    }
}
