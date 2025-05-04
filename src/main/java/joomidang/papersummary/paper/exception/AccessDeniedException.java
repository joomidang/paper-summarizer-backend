package joomidang.papersummary.paper.exception;

import joomidang.papersummary.paper.controller.response.PaperErrorCode;
import lombok.Getter;

@Getter
public class AccessDeniedException extends RuntimeException {
    private final PaperErrorCode errorCode;

    public AccessDeniedException(String message) {
        super(message);
        this.errorCode = PaperErrorCode.UNAUTHORIZED_ACCESS;
    }

    public AccessDeniedException() {
        this("해당 논문에 접근할 권한이 없습니다.");
    }

}