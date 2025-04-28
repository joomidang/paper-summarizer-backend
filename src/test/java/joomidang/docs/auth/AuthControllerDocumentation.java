package joomidang.docs.auth;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import joomidang.docs.util.RestDocsSupport;
import joomidang.papersummary.auth.controller.AuthController;
import joomidang.papersummary.auth.controller.request.TokenRefreshRequest;
import joomidang.papersummary.auth.controller.request.WithdrawRequest;
import joomidang.papersummary.auth.dto.TokenDto;
import joomidang.papersummary.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * AuthController API 문서화 테스트 클래스 RestDocsSupport를 상속받아 구현하여 인증 관련 API 문서화를 수행합니다.
 */
public class AuthControllerDocumentation extends RestDocsSupport {

    private final AuthService authService = Mockito.mock(AuthService.class);

    @Test
    @DisplayName("깃허브 로그인 API 문서화")
    void githubLogin() throws Exception {
        // given
        Mockito.when(authService.getAuthorizationUrl(Mockito.any()))
                .thenReturn("https://github.com/login/oauth/authorize?client_id=test&redirect_uri=test");

        // when
        ResultActions result = mockMvc.perform(
                get("/api/auth/github")
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andDo(createDocument(
                        responseHeaders(
                                headerWithName("Location").description("깃허브 인증 페이지 URL")
                        )
                ));
    }

    @Test
    @DisplayName("깃허브 콜백 API 문서화")
    void githubCallback() throws Exception {
        // given
        TokenDto tokenDto = TokenDto.builder()
                .accessToken("test_access_token")
                .refreshToken("test_refresh_token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();

        Mockito.when(authService.processOAuthCallback(Mockito.any(), Mockito.anyString()))
                .thenReturn(tokenDto);

        // when
        ResultActions result = mockMvc.perform(
                get("/api/auth/github/callback")
                        .param("code", "test_code")
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andDo(createDocument(
                        queryParameters(
                                parameterWithName("code").description("깃허브 인증 코드")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.STRING)
                                        .description("응답 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING)
                                        .description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT)
                                        .description("토큰 정보"),
                                fieldWithPath("data.accessToken").type(JsonFieldType.STRING)
                                        .description("액세스 토큰"),
                                fieldWithPath("data.refreshToken").type(JsonFieldType.STRING)
                                        .description("리프레시 토큰"),
                                fieldWithPath("data.tokenType").type(JsonFieldType.STRING)
                                        .description("토큰 타입"),
                                fieldWithPath("data.expiresIn").type(JsonFieldType.NUMBER)
                                        .description("토큰 만료 시간(초)")
                        )
                ));
    }

    @Test
    @DisplayName("회원 탈퇴 API 문서화")
    void withdrawAccount() throws Exception {
        // given
        WithdrawRequest request = new WithdrawRequest("test_authorization_code");
        String requestBody = convertToJson(request);

        // when
        ResultActions result = mockMvc.perform(
                delete("/api/auth/withdraw")
                        .header("X-AUTH-ID", "test_provider_uid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        result.andExpect(status().isOk())
                .andDo(createDocument(
                        requestFields(
                                fieldWithPath("authorizationCode").type(JsonFieldType.STRING)
                                        .description("인증 코드")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.STRING)
                                        .description("응답 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING)
                                        .description("응답 메시지")
                        )
                ));
    }

    @Test
    @DisplayName("토큰 갱신 API 문서화")
    void refreshToken() throws Exception {
        // given
        TokenDto tokenDto = TokenDto.builder()
                .accessToken("test_access_token")
                .refreshToken("test_refresh_token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();

        TokenRefreshRequest request = new TokenRefreshRequest("test_refresh_token");
        String requestBody = convertToJson(request);

        Mockito.when(authService.refreshToken(Mockito.anyString()))
                .thenReturn(tokenDto);

        // when
        ResultActions result = mockMvc.perform(
                post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        result.andExpect(status().isOk())
                .andDo(createDocument(
                        requestFields(
                                fieldWithPath("refreshToken").type(JsonFieldType.STRING)
                                        .description("리프레시 토큰")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.STRING)
                                        .description("응답 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING)
                                        .description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT)
                                        .description("토큰 정보"),
                                fieldWithPath("data.accessToken").type(JsonFieldType.STRING)
                                        .description("액세스 토큰"),
                                fieldWithPath("data.refreshToken").type(JsonFieldType.STRING)
                                        .description("리프레시 토큰"),
                                fieldWithPath("data.tokenType").type(JsonFieldType.STRING)
                                        .description("토큰 타입"),
                                fieldWithPath("data.expiresIn").type(JsonFieldType.NUMBER)
                                        .description("토큰 만료 시간(초)")
                        )
                ));
    }

    @Override
    protected Object initController() {
        return new AuthController(authService);
    }
}
