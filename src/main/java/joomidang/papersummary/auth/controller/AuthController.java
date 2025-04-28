package joomidang.papersummary.auth.controller;

import static joomidang.papersummary.auth.controller.response.AuthSuccessCode.LOGIN;
import static joomidang.papersummary.auth.controller.response.AuthSuccessCode.TOKEN_REFRESH;
import static joomidang.papersummary.auth.controller.response.AuthSuccessCode.WITHDRAW;

import java.net.URI;
import joomidang.papersummary.auth.controller.request.TokenRefreshRequest;
import joomidang.papersummary.auth.controller.request.WithdrawRequest;
import joomidang.papersummary.auth.dto.TokenDto;
import joomidang.papersummary.auth.resolver.Authenticated;
import joomidang.papersummary.auth.service.AuthService;
import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.users.entity.AuthProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<ApiResponse<TokenDto>> githubCallback(String code) {
        log.info("깃허브 콜백 받음. 코드는: {}", code);
        TokenDto tokenDto = authService.processOAuthCallback(AuthProvider.GITHUB, code);
        return ResponseEntity.ok()
                .body(ApiResponse.successWithData(LOGIN, tokenDto));
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
