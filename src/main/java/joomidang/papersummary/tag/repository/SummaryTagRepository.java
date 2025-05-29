package joomidang.papersummary.tag.repository;

import java.util.List;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.tag.entity.SummaryTag;
import joomidang.papersummary.tag.entity.Tag;
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
}
