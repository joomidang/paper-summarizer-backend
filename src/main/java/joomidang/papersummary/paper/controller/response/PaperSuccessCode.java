package joomidang.papersummary.paper.controller.response;

import joomidang.papersummary.common.controller.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaperSuccessCode implements SuccessCode {
    UPLOAD_SUCCESS("PAS-0001", "논문이 성공적으로 업로드되었습니다."),
    ANALYSIS_REQUESTED_SUCCESS("PAS-0002", "논문 분석 요청이 성공적으로 처리되었습니다."),
    PARSING_REQUESTED_SUCCESS("PAS-0003", "파싱 결과를 성공적으로 처리했습니다."),
    DELETE_SUCCESS("PAS-0004", "논문이 성공적으로 삭제되었습니다."),
    FETCH_SUCCESS("PAS-0005", "논문 정보를 성공적으로 조회했습니다.");

    private final String value;
    private final String message;
}
