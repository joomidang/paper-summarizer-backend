package joomidang.papersummary.auth.controller.response;

import joomidang.papersummary.common.controller.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;

/**
 * 테스트 인증 관련 성공 코드
 * <p>
 * 로컬 환경에서만 사용되는 테스트 인증 관련 성공 코드를 정의한다.
 */
@Getter
@RequiredArgsConstructor
@Profile("local")
public enum TestAuthSuccessCode implements SuccessCode {
    TEST_TOKEN_RETRIEVED("AUS-TEST-0001", "테스트 토큰이 조회되었습니다."),
    TEST_USER_RETRIEVED("AUS-TEST-0002", "테스트 사용자 정보가 조회되었습니다.");

    private final String value;
    private final String message;
}