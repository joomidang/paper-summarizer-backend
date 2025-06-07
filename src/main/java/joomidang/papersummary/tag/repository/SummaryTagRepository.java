package joomidang.papersummary.tag.repository;

import java.util.List;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.tag.entity.SummaryTag;
import joomidang.papersummary.tag.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SummaryTagRepository extends JpaRepository<SummaryTag, Long> {
    void deleteBySummary(Summary summary);

    boolean existsBySummaryAndTag(Summary summary, Tag tag);

    /**
     * 특정 요약본의 태그 목록 조회
     */
    @Query("SELECT st FROM SummaryTag st JOIN FETCH st.tag WHERE st.summary.id = :summaryId")
    List<SummaryTag> findBySummaryIdWithTag(@Param("summaryId") Long summaryId);

    /**
     * 특정 태그를 가진 요약본 목록 조회 (페이징, 정렬 지원)
     */
    @Query(value = "SELECT st.summary FROM SummaryTag st " +
            "JOIN FETCH st.summary.summaryStats " +
            "JOIN FETCH st.summary.member " +
            "WHERE st.tag.name = :tagName " +
            "AND st.summary.publishStatus = :publishStatus " +
            "AND st.summary.isDeleted = false",
            countQuery = "SELECT COUNT(st) FROM SummaryTag st " +
                    "WHERE st.tag.name = :tagName " +
                    "AND st.summary.publishStatus = :publishStatus " +
                    "AND st.summary.isDeleted = false")
    Page<Summary> findSummariesByTagName(
            @Param("tagName") String tagName,
            @Param("publishStatus") PublishStatus publishStatus,
            Pageable pageable);
}
