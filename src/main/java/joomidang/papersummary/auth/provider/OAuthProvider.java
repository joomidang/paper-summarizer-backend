package joomidang.papersummary.auth.provider;

import java.util.Map;
import joomidang.papersummary.users.entity.AuthProvider;
import joomidang.papersummary.users.entity.Member;

/**
 * OAuth 제공자를 위한 인터페이스. 이 인터페이스는 모든 OAuth 제공자가 구현해야 하는 공통 작업을 정의합니다.
 */
public interface OAuthProvider {

    /**
     * OAuth 제공자의 인증 URL을 가져옵니다.
     *
     * @return 인증 URL
     */
    String getAuthorizationUrl();

    /**
     * 인증 코드를 액세스 토큰으로 교환합니다.
     *
     * @param code 인증 코드
     * @return 액세스 토큰
     */
    String getAccessToken(String code);

    /**
     * OAuth 제공자로부터 사용자 정보를 가져옵니다.
     *
     * @param accessToken 액세스 토큰
     * @return 사용자 정보를 포함하는 맵
     */
    Map<String, Object> getUserInfo(String accessToken);

    /**
     * 사용자 정보에서 사용자 상세 정보를 추출합니다.
     *
     * @param userInfo OAuth 제공자로부터 받은 사용자 정보
     * @return 추출된 정보가 포함된 Users 객체
     */
    Member extractUserDetails(Map<String, Object> userInfo);

    /**
     * 인증 제공자 유형을 가져옵니다.
     */
    AuthProvider getProviderType();
}
