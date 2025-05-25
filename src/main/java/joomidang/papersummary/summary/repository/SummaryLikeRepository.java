package joomidang.papersummary.summary.repository;

import java.util.Optional;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.entity.SummaryLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SummaryLikeRepository extends JpaRepository<SummaryLike, Long> {

    @Query("SELECT s1 FROM SummaryLike s1 JOIN FETCH s1.summary s JOIN FETCH s.member "
            + "WHERE s1.member.id =:memberId AND s.publishStatus =:publishStatus "
            + "AND s.isDeleted = false ORDER BY s1.createdAt DESC")
    Page<SummaryLike> findByMemberIdWithSummary(@Param("memberId") Long memberId,
                                                @Param("publishStatus") PublishStatus publishStatus, Pageable pageable);

    //특정 사용자가 특정 요약본에 좋아요를 눌렀는지
    Optional<SummaryLike> findByMemberAndSummary(Member member, Summary summary);
}
