package joomidang.papersummary.paper.exception;

import joomidang.papersummary.paper.controller.response.PaperErrorCode;

public class InvalidFileTypeException extends RuntimeException {
    private final PaperErrorCode errorCode;

    public InvalidFileTypeException(String message) {
        super(message);
        this.errorCode = PaperErrorCode.INVALID_FILE_TYPE;
    }

    public InvalidFileTypeException() {
        this("지원하지 않는 파일 형식입니다. PDF 파일만 업로드 가능합니다.");
    }

    public PaperErrorCode getErrorCode() {
        return errorCode;
    }
}