package joomidang.papersummary.auth.service;

import joomidang.papersummary.auth.controller.request.WithdrawRequest;
import joomidang.papersummary.auth.dto.TokenDto;
import joomidang.papersummary.member.entity.AuthProvider;

public interface AuthService {
    /**
     * 지정된 OAuth 제공자에 대한 인증 URL을 가져옵니다
     *
     * @param provider OAuth 제공자
     * @return 인증 URL
     */
    String getAuthorizationUrl(AuthProvider provider);

    /**
     * 인증 코드로 OAuth 콜백을 처리합니다
     *
     * @param provider OAuth 제공자
     * @param code     제공자로부터 받은 인증 코드
     * @return JWT 토큰
     */
    TokenDto processOAuthCallback(AuthProvider provider, String code);

    /**
     * 시스템에서 사용자를 탈퇴시킵니다
     *
     * @param providerUid 제공자 사용자 ID
     * @param request     탈퇴 요청
     */
    void withdraw(String providerUid, WithdrawRequest request);

    /**
     * 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급합니다
     *
     * @param refreshToken 리프레시 토큰
     * @return 새로운 JWT 토큰
     */
    TokenDto refreshToken(String refreshToken);
}
