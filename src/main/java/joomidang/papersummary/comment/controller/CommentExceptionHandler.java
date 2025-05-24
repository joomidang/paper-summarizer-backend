package joomidang.papersummary.comment.controller;

import joomidang.papersummary.comment.exception.CommentAccessDeniedException;
import joomidang.papersummary.comment.exception.CommentNotFoundException;
import joomidang.papersummary.common.controller.response.ApiResponse;
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
public class CommentExceptionHandler {
    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCommentNotFoundException(CommentNotFoundException ex) {
        log.error("Comment not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(CommentAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCommentAccessDeniedException(CommentAccessDeniedException ex) {
        log.error("Comment access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }
}
