package joomidang.papersummary.paper.controller.response;

import joomidang.papersummary.common.controller.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaperSuccessCode implements SuccessCode {
    UPLOAD_SUCCESS("PAS-0001", "논문이 성공적으로 업로드되었습니다."),
    DELETE_SUCCESS("PAS-0002", "논문이 성공적으로 삭제되었습니다."),
    FETCH_SUCCESS("PAS-0003", "논문 정보를 성공적으로 조회했습니다.");

    private final String value;
    private final String message;
}
