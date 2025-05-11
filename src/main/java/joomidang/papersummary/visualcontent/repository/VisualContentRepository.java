package joomidang.papersummary.visualcontent.repository;

import java.util.List;
import java.util.Optional;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.visualcontent.entity.VisualContent;
import joomidang.papersummary.visualcontent.entity.VisualContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VisualContentRepository extends JpaRepository<VisualContent, Long> {
    List<VisualContent> findByPaperIdAndSummaryIsNull(Long paperId);

    List<VisualContent> findBySummaryAndType(Summary summary, VisualContentType type);

    @Query("SELECT MAX(v.position) FROM VisualContent v WHERE v.summary.id = :summaryId")
    Optional<Integer> findMaxPositionBySummaryId(Long summaryId);
}
