package joomidang.papersummary.paper.exception;

import joomidang.papersummary.paper.controller.response.PaperErrorCode;
import lombok.Getter;

@Getter
public class PaperNotFoundException extends RuntimeException {
    private final PaperErrorCode errorCode;

    public PaperNotFoundException(Long paperId) {
        super("해당 논문을 찾을 수 없습니다. id: " + paperId);
        this.errorCode = PaperErrorCode.PAPER_NOT_FOUND;
    }

}