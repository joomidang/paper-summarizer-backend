package joomidang.papersummary.auth.controller;

import static joomidang.papersummary.auth.controller.response.AuthSuccessCode.TOKEN_REFRESH;
import static joomidang.papersummary.auth.controller.response.AuthSuccessCode.WITHDRAW;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import joomidang.papersummary.auth.controller.request.TokenRefreshRequest;
import joomidang.papersummary.auth.controller.request.WithdrawRequest;
import joomidang.papersummary.auth.dto.TokenDto;
import joomidang.papersummary.auth.resolver.Authenticated;
import joomidang.papersummary.auth.service.AuthService;
import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.common.swagger.ApiResponseSchema;
import joomidang.papersummary.member.entity.AuthProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "인증 APIs")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Github 로그인", description = "Github OAuth 로그인 페이지로 리다이렉트합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "Github 인증 페이지로 리다이렉트")
    })
    @GetMapping("/github")
    public ResponseEntity<ApiResponse<Void>> githubLogin() {
        log.info("깃허브 로그인 요청");
        String redirectUrl = authService.getAuthorizationUrl(AuthProvider.GITHUB);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
    }


    @Operation(summary = "GitHub 콜백 처리", description = "GitHub OAuth 인증 후 콜백을 처리하고 토큰을 쿠키에 저장합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "인증 성공 후 메인 페이지로 리다이렉트")
    })
    @GetMapping("/github/callback")
    public ResponseEntity<Map<String, String>>githubCallback(String code, HttpServletResponse response)
//    public void githubCallback(String code, HttpServletResponse response)
            throws IOException {
        log.info("깃허브 콜백 받음. 코드는: {}", code);
        TokenDto tokenDto = authService.processOAuthCallback(AuthProvider.GITHUB, code);
        String accessToken = tokenDto.getAccessToken();
        String refreshToken = tokenDto.getRefreshToken();

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true) //개발 환경일때는 우선 false로 설정
                .secure(true) // true 이면 vercel의 경우만 가능 https환경에서만 동작
                .path("/")
                .sameSite("None") //운영 환경일때는 None으로 설정
                .maxAge(Duration.ofDays(1))
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true) //개발 환경일때는 우선 false로 설정
                .secure(true)
                .path("/")
                .sameSite("None")
                .maxAge(Duration.ofDays(7))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok()
                .headers(headers)
                .body(Map.of("message", "인증 성공"));
    }

    @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용하여 액세스 토큰을 갱신합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "토큰 갱신 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(
                                            value = "{\n" +
                                                    "  \"code\": \"AUS-0003\",\n" +
                                                    "  \"message\": \"토큰이 갱신되었습니다.\",\n" +
                                                    "  \"data\": {\n" +
                                                    "    \"accessToken\": \"test-access-token\",\n" +
                                                    "    \"refreshToken\": \"test-refresh-token\",\n" +
                                                    "    \"tokenType\": \"Bearer\",\n" +
                                                    "    \"expiresIn\": 3600\n" +
                                                    "  }\n" +
                                                    "}"

                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 리프레시 토큰"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "만료된 리프레시 토큰")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenDto>> refreshToken(
            @RequestBody TokenRefreshRequest request) {
        log.info("토큰 갱신 요청");
        TokenDto tokenDto = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok()
                .body(ApiResponse.successWithData(TOKEN_REFRESH, tokenDto));
    }


    @Operation(
            summary = "회원 탈퇴",
            description = "사용자 계정을 탈퇴 처리합니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원 탈퇴 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseSchema.class),
                            examples = {
                                    @ExampleObject(
                                            name = "회원 탈퇴 성공",
                                            summary = "회원 탈퇴 성공",
                                            value = "{\n" +
                                                    "  \"code\": \"" + "AUS-0002" + "\",\n" +
                                                    "  \"message\": \"" + "회원 탈퇴가 완료되었습니다." + "\",\n" +
                                                    "  \"data\": null\n" +
                                                    "}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음"
            )
    })
    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdrawAccount(
            @Authenticated String providerUid,
            @RequestBody WithdrawRequest request) {
        log.info("계정 탈퇴 요청");
        authService.withdraw(providerUid, request);
        return ResponseEntity.ok().body(ApiResponse.success(WITHDRAW));
    }
}
