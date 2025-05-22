package joomidang.papersummary.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class SuspiciousPathLoggerFilter extends OncePerRequestFilter {
    private static final List<String> BLOCKED_PATTERNS = List.of(
            "/setup.cgi", "/boaform/**", "/manager/**", "/get.php", "/download/**",
            "/ads.txt", "/app-ads.txt", "/sitemap.xml", "/sellers.json", "/geoserver/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        for (String pattern : BLOCKED_PATTERNS) {
            if (pathMatcher.match(pattern, uri)) {
                log.warn("👹 의심스러운 접근 감지: {} from IP: {}", uri, request.getRemoteAddr());
                break;
            }
        }
        filterChain.doFilter(request, response);
    }
}
