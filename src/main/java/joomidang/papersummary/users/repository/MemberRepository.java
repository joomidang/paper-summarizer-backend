package joomidang.papersummary.users.repository;

import java.util.Optional;
import joomidang.papersummary.users.entity.Member;
import joomidang.papersummary.users.entity.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 사용자 정보에 대한 데이터 액세스 인터페이스
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    /**
     * 인증 제공자와 제공자 ID로 사용자 조회
     *
     * @param authProvider 인증 제공자 (GITHUB, GOOGLE 등)
     * @param providerUid  제공자에서의 사용자 ID
     * @return 사용자 정보 (Optional)
     */
    Optional<Member> findByAuthProviderAndProviderUid(AuthProvider authProvider, String providerUid);

    /**
     * 제공자 ID로 사용자 조회
     *
     * @param providerUid
     * @return
     */
    Optional<Member> findByProviderUid(String providerUid);
}
