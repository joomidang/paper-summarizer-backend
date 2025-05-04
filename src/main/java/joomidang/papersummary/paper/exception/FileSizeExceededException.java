package joomidang.papersummary.paper.exception;

import joomidang.papersummary.paper.controller.response.PaperErrorCode;
import lombok.Getter;

@Getter
public class FileSizeExceededException extends RuntimeException {
    private final PaperErrorCode errorCode;

    public FileSizeExceededException(String message) {
        super(message);
        this.errorCode = PaperErrorCode.FILE_SIZE_EXCEEDED;
    }

    public FileSizeExceededException() {
        this("파일 크기가 제한을 초과했습니다.");
    }

}