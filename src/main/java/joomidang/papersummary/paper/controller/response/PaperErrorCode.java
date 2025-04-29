package joomidang.papersummary.paper.controller.response;

import joomidang.papersummary.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum PaperErrorCode implements ErrorCode {
    PAPER_NOT_FOUND("PAE-0001"),
    INVALID_FILE_TYPE("PAE-0002"),
    FILE_SIZE_EXCEEDED("PAE-0003"),
    FILE_UPLOAD_FAILED("PAE-0004"),
    UNAUTHORIZED_ACCESS("PAE-0005");

    private final String value;
}
