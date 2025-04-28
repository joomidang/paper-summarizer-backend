package joomidang.papersummary.auth.provider;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import joomidang.papersummary.users.entity.AuthProvider;
import joomidang.papersummary.users.entity.Member;
import joomidang.papersummary.users.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubOAuthProvider implements OAuthProvider {

    // GitHub API 엔드포인트
    private static final String GITHUB_AUTH_URL = "https://github.com/login/oauth/authorize";
    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String GITHUB_USER_URL = "https://api.github.com/user";
    private static final String GITHUB_EMAILS_URL = "https://api.github.com/user/emails";

    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.github.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.github.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.github.redirect-uri}")
    private String redirectUri;

    @Override
    public String getAuthorizationUrl() {
        log.debug("GitHub 인증 URL 생성 중");
        String authUrl = UriComponentsBuilder.fromUriString(GITHUB_AUTH_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "user:email read:user")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
        log.debug("GitHub 인증 URL: {}", authUrl);
        return authUrl;
    }

    @Override
    public String getAccessToken(String code) {
        log.debug("인증 코드를 액세스 토큰으로 교환 중");
        try {
            HttpHeaders headers = createHeaders(null, true);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("code", code);
            body.add("redirect_uri", redirectUri);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    GITHUB_TOKEN_URL,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<>() {
                    }
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("access_token")) {
                String accessToken = (String) responseBody.get("access_token");
                log.debug("액세스 토큰 획득 성공");
                return accessToken;
            } else {
                log.error("액세스 토큰 획득 실패: 응답 본문이 null이거나 access_token을 포함하지 않음");
                return null;
            }
        } catch (RestClientException e) {
            log.error("인증 코드를 액세스 토큰으로 교환하는 중 오류 발생", e);
            return null;
        }
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken) {
        log.debug("GitHub API에서 사용자 정보 가져오는 중");

        Map<String, Object> userInfo = makeApiRequest(
                GITHUB_USER_URL,
                HttpMethod.GET,
                accessToken,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                }
        );

        if (userInfo == null) {
            log.error("GitHub API에서 사용자 정보 가져오기 실패");
            return null;
        }

        // 이메일이 null이거나 비어있는 경우 이메일 엔드포인트에서 가져옴
        if (userInfo.get("email") == null || ((String) userInfo.get("email")).isEmpty()) {
            log.debug("이메일이 null이거나 비어있음, 이메일 엔드포인트에서 가져오는 중");
            String email = getPrimaryEmail(accessToken);
            if (email != null && !email.isEmpty()) {
                userInfo.put("email", email);
                log.debug("이메일 엔드포인트에서 이메일 추가: {}", email);
            }
        }

        return userInfo;
    }

    private HttpHeaders createHeaders(String accessToken, boolean isFormUrlEncoded) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        if (isFormUrlEncoded) {
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        }

        if (accessToken != null && !accessToken.isEmpty()) {
            headers.setBearerAuth(accessToken);
        }

        return headers;
    }

    @Override
    public Member extractUserDetails(Map<String, Object> userInfo) {
        if (userInfo == null) {
            log.error("사용자 세부 정보를 추출할 수 없음: userInfo가 null임");
            return null;
        }

        log.debug("GitHub 사용자 정보에서 사용자 세부 정보 추출 중");

        String providerUid = String.valueOf(userInfo.get("id"));
        String name = (String) userInfo.get("name");
        String login = (String) userInfo.get("login");
        String email = (String) userInfo.get("email");
        String profileImage = (String) userInfo.get("avatar_url");

        // provider_data 필드를 위해 GitHub 사용자 정보를 문자열로 변환
        String providerData = userInfo.toString();

        // 이름이 null인 경우 로그인을 사용자 이름으로 사용
        String username = (name != null && !name.isEmpty()) ? name : login;

        log.debug("추출된 사용자 세부 정보 - 사용자 이름: {}, 이메일: {}", username, email);

        return Member.builder()
                .name(username)
                .email(email)
                .profileImage(profileImage)
                .authProvider(AuthProvider.GITHUB)
                .providerUid(providerUid)
                .providerData(providerData)
                .role(Role.USER)
                .build();
    }

    private <T> T makeApiRequest(String url, HttpMethod method, String accessToken,
                                 org.springframework.core.ParameterizedTypeReference<T> responseType) {
        try {
            HttpHeaders headers = createHeaders(accessToken, false);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    method,
                    entity,
                    responseType
            );

            return response.getBody();
        } catch (RestClientException e) {
            log.error("{}에 API 요청 중 오류 발생: {}", url, e.getMessage());
            return null;
        }
    }

    private String getPrimaryEmail(String accessToken) {
        log.debug("GitHub API에서 기본 이메일 가져오는 중");

        List<Map<String, Object>> emails = makeApiRequest(
                GITHUB_EMAILS_URL,
                HttpMethod.GET,
                accessToken,
                new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {
                }
        );

        if (emails == null || emails.isEmpty()) {
            log.debug("GitHub API에서 이메일을 찾을 수 없음");
            return null;
        }

        // 기본 및 인증된 이메일 찾기
        String primaryVerifiedEmail = findEmailByAttributes(emails, true, true);
        if (primaryVerifiedEmail != null) {
            return primaryVerifiedEmail;
        }

        // 기본 인증 이메일이 없는 경우 인증된 이메일 찾기
        String verifiedEmail = findEmailByAttributes(emails, null, true);
        if (verifiedEmail != null) {
            return verifiedEmail;
        }

        // 인증된 이메일이 없는 경우 첫 번째 이메일 반환
        log.debug("인증된 이메일을 찾을 수 없음, 첫 번째 이메일 반환");
        return (String) emails.get(0).get("email");
    }

    private String findEmailByAttributes(List<Map<String, Object>> emails, Boolean primary, Boolean verified) {
        for (Map<String, Object> emailData : emails) {
            Boolean isPrimary = (Boolean) emailData.get("primary");
            Boolean isVerified = (Boolean) emailData.get("verified");

            boolean primaryMatch = primary == null || (isPrimary != null && isPrimary.equals(primary));
            boolean verifiedMatch = verified == null || (isVerified != null && isVerified.equals(verified));

            if (primaryMatch && verifiedMatch) {
                String email = (String) emailData.get("email");
                log.debug("일치하는 이메일 찾음: {} (기본: {}, 인증됨: {})",
                        email, isPrimary, isVerified);
                return email;
            }
        }

        return null;
    }

    @Override
    public AuthProvider getProviderType() {
        return AuthProvider.GITHUB;
    }
}