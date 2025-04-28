package joomidang.papersummary.auth.provider;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import joomidang.papersummary.users.entity.AuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * OAuth 제공자를 생성하기 위한 팩토리입니다. 이 팩토리는 여러 OAuth 제공자를 관리하고 제공자 유형에 따라 적절한 제공자를 반환합니다.
 */
@Component
@RequiredArgsConstructor
public class OAuthProviderFactory {

    private final List<OAuthProvider> oauthProviders;
    private final Map<AuthProvider, OAuthProvider> providerMap = new HashMap<>();

    /**
     * 모든 제공자가 주입된 후 제공자 맵을 초기화합니다.
     */
    @PostConstruct
    public void init() {
        for (OAuthProvider provider : oauthProviders) {
            providerMap.put(provider.getProviderType(), provider);
        }
    }

    /**
     * 지정된 제공자 유형에 대한 OAuth 제공자를 가져옵니다.
     */
    public OAuthProvider getProvider(AuthProvider providerType) {
        OAuthProvider provider = providerMap.get(providerType);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported OAuth provider: " + providerType);
        }
        return provider;
    }
}
