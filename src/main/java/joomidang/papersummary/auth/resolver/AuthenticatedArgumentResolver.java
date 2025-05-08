package joomidang.papersummary.auth.resolver;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import joomidang.papersummary.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
@RequiredArgsConstructor
@Component
public class AuthenticatedArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtTokenProvider tokenProvider;

    @Override
    public boolean supportsParameter(final MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Authenticated.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        // 1. 쿠키에서 토큰 확인
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        log.debug("헤더 Authorization: {}", webRequest.getHeader("Authorization"));

        if (request != null && request.getCookies() != null) {
            log.debug("쿠키 정보: {}", Arrays.toString(request.getCookies()));
            Optional<Cookie> tokenCookie = Arrays.stream(request.getCookies())
                    .filter(c -> "accessToken".equals(c.getName()))
                    .findFirst();

            if (tokenCookie.isPresent()) {
                String token = "Bearer " + tokenCookie.get().getValue(); // Bearer 접두사 추가
                return tokenProvider.getUserId(token);
            }
        } else {
            log.debug("요청에 쿠키가 없음");
        }
        // 2. 헤더에서 토큰 확인
        final String authorizationHeader = webRequest.getHeader("Authorization");
        if (StringUtils.hasText(authorizationHeader)) {
            return tokenProvider.getUserId(authorizationHeader);
        }

        // 3. SecurityContext에서 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User user) {
                return user.getUsername();
            }
        }

        throw new BadCredentialsException("인증 정보를 찾을 수 없습니다");
    }
}
