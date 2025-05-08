package joomidang.papersummary.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import joomidang.papersummary.auth.controller.request.TokenRefreshRequest;
import joomidang.papersummary.auth.controller.request.WithdrawRequest;
import joomidang.papersummary.auth.dto.TokenDto;
import joomidang.papersummary.auth.resolver.Authenticated;
import joomidang.papersummary.auth.service.AuthService;
import joomidang.papersummary.member.entity.AuthProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AuthService authService;

    /**
     * 테스트용 인증 어노테이션 리졸버 X-AUTH-ID 헤더에서 사용자 ID를 추출하여 @Authenticated 어노테이션이 붙은 파라미터에 주입
     */
    static class TestAuthenticatedArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(Authenticated.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {
            return webRequest.getHeader("X-AUTH-ID");
        }
    }

    private TokenDto tokenDto;

    @BeforeEach
    void setUp() {
        // 의존성 초기화
        objectMapper = new ObjectMapper();
        authService = mock(AuthService.class);

        // 컨트롤러 설정
        AuthController authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setCustomArgumentResolvers(new TestAuthenticatedArgumentResolver())
                .build();

        // 테스트에 사용할 토큰 DTO 생성
        tokenDto = TokenDto.builder()
                .accessToken("test-access-token")
                .refreshToken("test-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();
    }

    @Test
    @DisplayName("GitHub 로그인 요청 테스트")
    void githubLogin() throws Exception {
        // given
        String redirectUrl = "https://github.com/login/oauth/authorize?client_id=test-client-id&redirect_uri=test-redirect-uri";
        when(authService.getAuthorizationUrl(AuthProvider.GITHUB)).thenReturn(redirectUrl);

        // when & then
        mockMvc.perform(get("/api/auth/github")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", redirectUrl));
    }

    @Test
    @DisplayName("GitHub 콜백 처리 테스트 - 쿠키 응답 및 리다이렉트")
    void githubCallback() throws Exception {
        // given
        String code = "test-code";
        TokenDto tokenDto = TokenDto.builder()
                .accessToken("test-access-token")
                .refreshToken("test-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();

        when(authService.processOAuthCallback(eq(AuthProvider.GITHUB), eq(code)))
                .thenReturn(tokenDto);

        // when & then
        mockMvc.perform(get("/api/auth/github/callback")
                        .param("code", code)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost:3000/callback"))
                .andExpect(header().stringValues("Set-Cookie", Matchers.hasItems(
                        Matchers.containsString("accessToken=test-access-token"),
                        Matchers.containsString("refreshToken=test-refresh-token")
                )));
    }


    @Test
    @DisplayName("회원 탈퇴 테스트")
    void withdrawAccount() throws Exception {
        // given
        String providerUid = "test-provider-uid";
        WithdrawRequest request = new WithdrawRequest("test-authorization-code");
        doNothing().when(authService).withdraw(eq(providerUid), any(WithdrawRequest.class));

        // when & then
        mockMvc.perform(delete("/api/auth/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-AUTH-ID", providerUid)  // 인증된 사용자 ID를 헤더에 추가
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUS-0002"))
                .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."));
    }

    @Test
    @DisplayName("토큰 갱신 테스트")
    void refreshToken() throws Exception {
        // given
        String refreshToken = "test-refresh-token";
        TokenRefreshRequest request = new TokenRefreshRequest(refreshToken);
        when(authService.refreshToken(eq(refreshToken))).thenReturn(tokenDto);

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUS-0003"))
                .andExpect(jsonPath("$.message").value("토큰이 갱신되었습니다."))
                .andExpect(jsonPath("$.data.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("test-refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600));
    }
}
