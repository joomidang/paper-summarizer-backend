package joomidang.papersummary.summary.exception;

import joomidang.papersummary.summary.controller.response.SummaryErrorCode;
import lombok.Getter;

@Getter
public class SummaryNotFoundException extends RuntimeException {
    private final SummaryErrorCode errorCode;

    public SummaryNotFoundException(Long summaryId) {
        super("해당 요약을 찾을 수 없습니다. id: " + summaryId);
        this.errorCode = SummaryErrorCode.SUMMARY_NOT_FOUND;
    }
}