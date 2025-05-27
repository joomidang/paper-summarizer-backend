package joomidang.papersummary.summary.repository;

import java.util.List;
import java.util.Optional;
import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, Long> {
    @Query("SELECT s FROM Summary s WHERE s.id = :id")
    Optional<Summary> findByIdWithoutStats(@Param("id") Long id);

    @Query("SELECT s FROM Summary s JOIN FETCH s.summaryStats WHERE s.id=:id")
    Optional<Summary> findByIdWithStats(@Param("id") Long id);

    boolean existsByPaper(Paper paper);

    @Query(value = "SELECT s FROM Summary s WHERE s.member.id = :memberId AND s.isDeleted = false",
            countQuery = "SELECT COUNT(s) FROM Summary s WHERE s.member.id = :memberId AND s.isDeleted = false")
    Page<Summary> findByMemberIdWithStats(@Param("memberId") Long memberId, Pageable pageable);


    @Query(value = "SELECT s FROM Summary s JOIN FETCH s.summaryStats st JOIN FETCH s.member m "
            + "WHERE s.publishStatus = :publishStatus AND s.isDeleted = false "
            + "ORDER BY ("
            + "COALESCE(st.likeCount,0)*0.5 +"
            + "COALESCE(st.commentCount,0)*0.3 +"
            + "COALESCE(st.viewCount, 0)*0.2"
            + ") DESC, s.updatedAt DESC",
            countQuery = "SELECT COUNT(s) FROM Summary s WHERE s.publishStatus = :publishStatus AND s.isDeleted = false")
    Page<Summary> findPopularSummariesByPublishStatus(@Param("publishStatus") PublishStatus publishStatus,
                                                      Pageable pageable);

    @Query("SELECT s.id, (COALESCE(st.likeCount, 0)*0.5 +"
            + "COALESCE(st.commentCount, 0)*0.3 + "
            + "COALESCE(st.viewCount, 0)*0.2) "
            + "FROM Summary s "
            + "LEFT JOIN s.summaryStats st "
            + "WHERE s.id IN :summaryIds")
    List<Object[]> calculatePopularityScores(@Param("summaryIds") List<Long> summaryIds);

    long countByMemberId(Long memberId);
}
