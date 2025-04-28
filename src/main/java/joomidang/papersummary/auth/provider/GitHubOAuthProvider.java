package joomidang.papersummary.auth.provider;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import joomidang.papersummary.users.entity.AuthProvider;
import joomidang.papersummary.users.entity.Member;
import joomidang.papersummary.users.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubOAuthProvider implements OAuthProvider {

    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.github.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.github.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.github.redirect-uri}")
    private String redirectUri;

    @Override
    public String getAuthorizationUrl() {
        return UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "user:email read:user")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    @Override
    public String getAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://github.com/login/oauth/access_token",
                HttpMethod.POST,
                request,
                Map.class
        );

        return (String) response.getBody().get("access_token");
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.github.com/user",
                HttpMethod.GET,
                entity,
                Map.class
        );

        return response.getBody();
    }

    @Override
    public Member extractUserDetails(Map<String, Object> userInfo) {
        String providerUid = String.valueOf(userInfo.get("id"));
        String name = (String) userInfo.get("name");
        String login = (String) userInfo.get("login");
        String email = (String) userInfo.get("email");
        String profileImage = (String) userInfo.get("avatar_url");

        // GitHub 사용자 정보를 JSON 문자열로 변환 (provider_data 필드용)
        String providerData = userInfo.toString();

        // Use login as username if name is null
        String username = (name != null) ? name : login;

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

    @Override
    public AuthProvider getProviderType() {
        return AuthProvider.GITHUB;
    }
}
