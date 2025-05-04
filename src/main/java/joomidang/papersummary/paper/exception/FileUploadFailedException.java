package joomidang.papersummary.paper.exception;

import joomidang.papersummary.paper.controller.response.PaperErrorCode;
import lombok.Getter;

@Getter
public class FileUploadFailedException extends RuntimeException {
    private final PaperErrorCode errorCode;

    public FileUploadFailedException(String message) {
        super(message);
        this.errorCode = PaperErrorCode.FILE_UPLOAD_FAILED;
    }

    public FileUploadFailedException() {
        this("파일 업로드에 실패했습니다.");
    }

}