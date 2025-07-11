package joomidang.papersummary.paper.repository;

import java.util.List;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.paper.entity.Paper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 논문 정보에 대한 데이터 액세스 인터페이스
 */
@Repository
public interface PaperRepository extends JpaRepository<Paper, Long> {
    List<Paper> findAllByMember(Member member);
}
