package joomidang.papersummary.paper.controller;

import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.paper.exception.AccessDeniedException;
import joomidang.papersummary.paper.exception.FileSizeExceededException;
import joomidang.papersummary.paper.exception.FileUploadFailedException;
import joomidang.papersummary.paper.exception.InvalidFileTypeException;
import joomidang.papersummary.paper.exception.PaperNotFoundException;
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
public class PaperExceptionHandler {

    @ExceptionHandler(PaperNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaperNotFoundException(PaperNotFoundException ex) {
        log.error("Paper not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidFileTypeException(InvalidFileTypeException ex) {
        log.error("Invalid file type: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(FileSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileSizeExceededException(FileSizeExceededException ex) {
        log.error("File size exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(FileUploadFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileUploadFailedException(FileUploadFailedException ex) {
        log.error("File upload failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedAccessException(AccessDeniedException ex) {
        log.error("Unauthorized access: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }
}