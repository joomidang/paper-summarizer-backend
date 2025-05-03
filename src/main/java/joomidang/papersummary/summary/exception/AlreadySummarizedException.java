package joomidang.papersummary.summary.exception;

import joomidang.papersummary.summary.controller.response.SummaryErrorCode;
import lombok.Getter;

@Getter
public class AlreadySummarizedException extends RuntimeException {
    private final SummaryErrorCode errorCode;

    public AlreadySummarizedException(String message) {
        super(message);
        this.errorCode = SummaryErrorCode.SUMMARY_ALREADY_EXISTS;
    }
}
