package joomidang.papersummary.auth.config;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import joomidang.papersummary.auth.dto.TokenDto;
import joomidang.papersummary.auth.security.JwtTokenProvider;
import joomidang.papersummary.member.entity.AuthProvider;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.entity.Role;
import joomidang.papersummary.member.service.MemberService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

/**
 * 로컬 환경에서 테스트를 위한 사용자 및 토큰 생성 설정
 * <p>
 * 애플리케이션 시작 시 테스트 사용자와 토큰을 생성하여 저장한다.
 */
@Slf4j
@Component
@Profile("local")
@Tag(name = "Test", description = "Local Test User Configuration")
@RequiredArgsConstructor
public class TestUserConfig {

    private final MemberService memberService;
    private final JwtTokenProvider tokenProvider;

    @Getter
    private TokenDto testToken;

    @Getter
    private Member testUser;

    /**
     * 애플리케이션 시작 시 테스트 사용자와 토큰을 생성한다.
     */

    @PostConstruct

    public void init() {
        log.info("로컬 환경용 테스트 사용자 및 토큰 생성 시작");
        createTestUser("1111@example.com", "UserA");
        createTestUser("2222@example.com", "UserB");
        createTestUser("3333@example.com", "UserC");
        createTestUser("4444@example.com", "UserD");
        createTestToken();
        log.info("로컬 환경용 테스트 사용자 및 토큰 생성 완료");
    }

    /**
     * 테스트 사용자를 생성하거나 기존 사용자를 조회한다.
     */
    private void createTestUser(String email, String name) {
        // 고정된 테스트 사용자 ID 사용
        String testProviderUid = "test-user-local" + name;

        // 기존 테스트 사용자가 있는지 확인
        memberService.findByProviderAndProviderUid(AuthProvider.LOCAL, testProviderUid)
                .ifPresentOrElse(
                        existingUser -> {
                            log.info("기존 테스트 사용자 사용: {}", existingUser.getEmail());
                            testUser = existingUser;
                        },
                        () -> {
                            // 새 테스트 사용자 생성
                            Member newUser = Member.builder()
                                    .email(email)
                                    .name(name)
                                    .authProvider(AuthProvider.LOCAL)
                                    .providerUid(testProviderUid)
                                    .role(Role.USER)
                                    .profileImage("https://example.com/profile.jpg")
                                    .build();

                            testUser = memberService.save(newUser);
                            log.info("새 테스트 사용자 생성: {}", testUser.getEmail());
                        }
                );
    }

    /**
     * 테스트 사용자를 위한 토큰을 생성한다.
     */
    private void createTestToken() {
        // Spring Security UserDetails 생성
        User userDetails = new User(
                testUser.getProviderUid(),
                "",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + testUser.getRole().name()))
        );

        // Authentication 객체 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        // 토큰 생성
        testToken = tokenProvider.createToken(authentication);
        log.info("테스트 토큰 생성 완료");
    }

}
