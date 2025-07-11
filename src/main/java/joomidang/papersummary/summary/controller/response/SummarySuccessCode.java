package joomidang.papersummary.summary.controller.response;

import joomidang.papersummary.common.controller.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SummarySuccessCode implements SuccessCode {
    SUMMARY_CREATED("SUS-0001", "요약이 성공적으로 생성되었습니다."),
    SUMMARY_UPDATED("SUS-0002", "요약이 성공적으로 업데이트되었습니다."),
    SUMMARY_FETCHED("SUS-0003", "요약 정보를 성공적으로 조회했습니다."),
    SUMMARY_PUBLISH("SUS-0004", "요약을 성공적으로 발행했습니다."),


    SUMMARY_DELETED("SUS-0005", "요약이 성공적으로 삭제되었습니다."),
    SUMMARY_VERSION_DELETED("SUS-0006", "요약 버전이 성공적으로 삭제되었습니다."),
    SUMMARY_VERSION_FETCHED("SUS-0007", "요약 버전 목록을 성공적으로 조회했습니다."),

    SUMMARY_LIKE("SUS-0008", "요약에 성공적으로 좋아요를 추가했습니다.");

    private final String value;
    private final String message;
}