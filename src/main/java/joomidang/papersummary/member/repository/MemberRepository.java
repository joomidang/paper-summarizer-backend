package joomidang.papersummary.member.repository;

import java.util.Optional;
import joomidang.papersummary.member.entity.AuthProvider;
import joomidang.papersummary.member.entity.Member;
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

    boolean existsByName(String name);

    /**
     * 특정 ID를 제외하고 해당 이름을 가진 회원이 존재하는지 확인 프로필 수정 시 본인을 제외한 닉네임 중복 검사에 사용
     */
    boolean existsByNameAndIdNot(String username, Long memberId);
}
