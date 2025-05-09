package joomidang.papersummary.auth.controller;

import joomidang.papersummary.auth.config.TestUserConfig;
import joomidang.papersummary.auth.controller.response.TestAuthSuccessCode;
import joomidang.papersummary.auth.dto.TokenDto;
import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.member.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로컬 환경에서 테스트를 위한 인증 컨트롤러
 * <p>
 * 테스트 사용자 정보와 토큰을 조회할 수 있는 엔드포인트를 제공한다.
 * <p>
 * 이 컨트롤러는 로컬 환경에서만 활성화된다.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/test")
@Profile("local")
@RequiredArgsConstructor
public class TestAuthController {

    private final TestUserConfig testUserConfig;

    /**
     * 테스트 사용자의 토큰을 조회한다.
     * <p>
     * 로컬 환경에서 로그인 과정 없이 인증이 필요한 API를 테스트할 때 사용할 수 있다.
     */
    @GetMapping("/token")
    public ResponseEntity<ApiResponse<TokenDto>> getTestToken() {
        log.info("테스트 토큰 조회 요청");
        TokenDto tokenDto = testUserConfig.getTestToken();
        return ResponseEntity.ok(ApiResponse.successWithData(TestAuthSuccessCode.TEST_TOKEN_RETRIEVED, tokenDto));
    }

    /**
     * 테스트 사용자 정보를 조회한다.
     */
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<TestUserResponse>> getTestUser() {
        log.info("테스트 사용자 정보 조회 요청");
        Member testUser = testUserConfig.getTestUser();
        TestUserResponse response = new TestUserResponse(
                testUser.getId(),
                testUser.getName(),
                testUser.getEmail(),
                testUser.getProviderUid(),
                testUser.getRole().name()
        );
        return ResponseEntity.ok(ApiResponse.successWithData(TestAuthSuccessCode.TEST_USER_RETRIEVED, response));
    }

    private record TestUserResponse(
            Long id,
            String name,
            String email,
            String providerUid,
            String role
    ) {
    }
}
