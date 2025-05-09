package joomidang.papersummary.auth.controller;

import static joomidang.papersummary.auth.controller.response.AuthSuccessCode.TOKEN_REFRESH;
import static joomidang.papersummary.auth.controller.response.AuthSuccessCode.WITHDRAW;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import joomidang.papersummary.auth.controller.request.TokenRefreshRequest;
import joomidang.papersummary.auth.controller.request.WithdrawRequest;
import joomidang.papersummary.auth.dto.TokenDto;
import joomidang.papersummary.auth.resolver.Authenticated;
import joomidang.papersummary.auth.service.AuthService;
import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.member.entity.AuthProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/github")
    public ResponseEntity<ApiResponse<Void>> githubLogin() {
        log.info("깃허브 로그인 요청");
        String redirectUrl = authService.getAuthorizationUrl(AuthProvider.GITHUB);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
    }

    @GetMapping("/github/callback")
    public void githubCallback(String code, HttpServletResponse response)
            throws IOException {
        log.info("깃허브 콜백 받음. 코드는: {}", code);
        TokenDto tokenDto = authService.processOAuthCallback(AuthProvider.GITHUB, code);
        String accessToken = tokenDto.getAccessToken();
        String refreshToken = tokenDto.getRefreshToken();

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(false) //개발 환경일때는 우선 false로 설정
                .secure(false) // true 이면 vercel의 경우만 가능 https환경에서만 동작
                .path("/")
                .sameSite("Lax") //운영 환경일때는 None으로 설정
                .maxAge(Duration.ofDays(1))
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(false) //개발 환경일때는 우선 false로 설정
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofDays(7))
                .build();

        // 쿠키 추가
        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        response.sendRedirect("http://localhost:3000/");
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenDto>> refreshToken(
            @RequestBody TokenRefreshRequest request) {
        log.info("토큰 갱신 요청");
        TokenDto tokenDto = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok()
                .body(ApiResponse.successWithData(TOKEN_REFRESH, tokenDto));
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdrawAccount(
            @Authenticated String providerUid,
            @RequestBody WithdrawRequest request) {
        log.info("계정 탈퇴 요청");
        authService.withdraw(providerUid, request);
        return ResponseEntity.ok().body(ApiResponse.success(WITHDRAW));
    }
}
