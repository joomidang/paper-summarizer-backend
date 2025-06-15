package joomidang.papersummary.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String jwt = resolveToken(request);
        String requestURI = request.getRequestURI();

        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            Authentication authentication = tokenProvider.getAuthentication(jwt);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("인증 정보를 시큐리티 컨텍스트에 저장했습니다. '{}', uri: {}", authentication.getName(), requestURI);
        } else {
            log.debug("유효한 JWT 토큰을 찾을 수 없습니다., uri: {}", requestURI);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        //쿠키에서 accessToken 찾기
        if (request.getCookies() != null) {
            log.debug("요청에 쿠키가 존재함: {}", Arrays.toString(request.getCookies()));
            return Arrays.stream(request.getCookies())
                    .filter(c -> "accessToken".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        } else {
            log.debug("요청에 쿠키가 없음: {}", request.getRequestURI());
        }
        //헤더에서 accessToken 찾기
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            log.debug("헤더에서 토큰 발견: {}", bearerToken);
            return bearerToken.substring(7);
        }

        return null;
    }
}