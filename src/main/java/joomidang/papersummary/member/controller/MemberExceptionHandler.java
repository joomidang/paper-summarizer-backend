package joomidang.papersummary.member.controller;

import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.common.exception.ErrorCode;
import joomidang.papersummary.member.controller.response.MemberErrorCode;
import joomidang.papersummary.member.exception.MemberDuplicateException;
import joomidang.papersummary.member.exception.MemberNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "joomidang.papersummary.member.controller")
public class MemberExceptionHandler {

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(MemberNotFoundException ex) {
        log.error("사용자 찾을 수 없음: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * 유효성 검증 에러
     * @param ex
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidException(MethodArgumentNotValidException ex) {
//        log.error("유효성 검사 실패(핸들러 진입): {}", ex.getMessage(), ex);

        ErrorCode errorCode = MemberErrorCode.VALIDATION_ERROR;
        String errorMessage;
        try {
            errorMessage = ex.getBindingResult().getFieldErrors().stream()
                    .map(error -> {
                        log.error("필드 에러: {}={}", error.getField(), error.getDefaultMessage());
                        return error.getDefaultMessage();
                    })
                    .findFirst()
                    .orElse("입력값이 올바르지 않습니다.");
        } catch (Exception e) {
            log.error("errorMessage 생성 중 에러", e);
            errorMessage = "입력값이 올바르지 않습니다.";
        }

        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(errorCode, errorMessage));
    }


    /**
     * 닉네임 중복
     * @param ex
     * @return
     */
    @ExceptionHandler(MemberDuplicateException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateException(MemberDuplicateException ex) {
        log.error("닉네임 중복 오류: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }

}
