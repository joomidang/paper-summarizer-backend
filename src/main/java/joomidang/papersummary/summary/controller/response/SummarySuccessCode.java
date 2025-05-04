package joomidang.papersummary.summary.controller.response;

import joomidang.papersummary.common.controller.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SummarySuccessCode implements SuccessCode {
    SUMMARY_CREATED("SUS-0001", "요약이 성공적으로 생성되었습니다."),
    SUMMARY_UPDATED("SUS-0002", "요약이 성공적으로 업데이트되었습니다."),
    SUMMARY_DELETED("SUS-0003", "요약이 성공적으로 삭제되었습니다."),
    SUMMARY_FETCHED("SUS-0004", "요약 정보를 성공적으로 조회했습니다.");

    private final String value;
    private final String message;
}