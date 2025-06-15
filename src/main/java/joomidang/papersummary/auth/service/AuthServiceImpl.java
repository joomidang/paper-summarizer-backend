package joomidang.papersummary.auth.service;

import java.util.Collections;
import java.util.Map;
import joomidang.papersummary.auth.controller.request.WithdrawRequest;
import joomidang.papersummary.auth.dto.TokenDto;
import joomidang.papersummary.auth.provider.OAuthProvider;
import joomidang.papersummary.auth.provider.OAuthProviderFactory;
import joomidang.papersummary.auth.security.JwtTokenProvider;
import joomidang.papersummary.member.entity.AuthProvider;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider tokenProvider;
    private final MemberService memberService;
    private final OAuthProviderFactory providerFactory;

    @Override
    public String getAuthorizationUrl(AuthProvider provider) {
        OAuthProvider oauthProvider = providerFactory.getProvider(provider);
        return oauthProvider.getAuthorizationUrl();
    }

    @Override
    public TokenDto processOAuthCallback(AuthProvider provider, String code) {
        OAuthProvider oauthProvider = providerFactory.getProvider(provider);

        // 인증 코드를 액세스 토큰으로 교환
        String accessToken = oauthProvider.getAccessToken(code);

        // 제공자로부터 사용자 정보 가져오기
        Map<String, Object> userInfo = oauthProvider.getUserInfo(accessToken);

        // 사용자 상세 정보 추출
        Member memberDetails = oauthProvider.extractUserDetails(userInfo);

        // 사용자 저장 또는 업데이트
        Member member = saveOrUpdateMember(memberDetails);

        // 인증 객체 생성
        Authentication authentication = createAuthentication(member);

        // JWT 토큰 생성
        return tokenProvider.createToken(authentication);
    }

    @Override
    public void withdraw(String providerUid, WithdrawRequest request) {
        memberService.delete(providerUid);
    }

    @Override
    public TokenDto refreshToken(String refreshToken) {
        // 리프레시 토큰 유효성 검증
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        // 리프레시 토큰에서 사용자 ID 추출
        String userId = tokenProvider.getUserId(refreshToken);

        // 인증 객체 생성
        Authentication authentication = createAuthenticationFromUserId(userId);

        // 새로운 토큰 생성
        return tokenProvider.createToken(authentication);
    }

    private Authentication createAuthenticationFromUserId(String userId) {
        // 사용자 정보 조회
        Member member = memberService.findByProviderUid(userId);

        // Spring Security UserDetails 생성
        UserDetails userDetails = User.builder()
                .username(member.getProviderUid())
                .password("")
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + member.getRole().name())))
                .build();

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    private Member saveOrUpdateMember(Member memberDetails) {
        return memberService.saveOrUpdate(memberDetails);
    }

    private Authentication createAuthentication(Member member) {
        // Spring Security UserDetails 생성
        UserDetails userDetails = User.builder()
                .username(member.getProviderUid()) // 제공자 ID를 사용자명으로 사용
                .password("") // OAuth2를 위한 비밀번호 없음
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + member.getRole().name())))
                .build();

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }
}
