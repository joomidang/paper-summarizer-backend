package joomidang.papersummary.summary.repository;

import joomidang.papersummary.summary.entity.SummaryStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SummaryStatsRepository extends JpaRepository<SummaryStats, Long> {
    @Modifying
    @Query("UPDATE SummaryStats s SET s.viewCount = s.viewCount + 1, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :summaryId")
    int increaseViewCount(@Param("summaryId") Long summaryId);

    @Modifying
    @Query("UPDATE SummaryStats s SET s.likeCount = s.likeCount + 1, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :summaryId")
    int increaseLikeCount(@Param("summaryId") Long summaryId);

    @Modifying
    @Query("UPDATE SummaryStats s SET s.likeCount = s.likeCount - 1, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :summaryId AND s.likeCount > 0")
    int decreaseLikeCount(@Param("summaryId") Long summaryId);

    @Modifying
    @Query("UPDATE SummaryStats s SET s.commentCount = s.commentCount + 1, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :summaryId")
    int increaseCommentCount(@Param("summaryId") Long summaryId);

    @Modifying
    @Query("UPDATE SummaryStats s SET s.commentCount = s.commentCount - 1, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :summaryId AND s.commentCount > 0")
    int decreaseCommentCount(@Param("summaryId") Long summaryId);

    @Modifying
    @Query("DELETE FROM SummaryStats s WHERE s.summary.id = :summaryId")
    void deleteBySummaryId(@Param("summaryId") Long summaryId);
}
