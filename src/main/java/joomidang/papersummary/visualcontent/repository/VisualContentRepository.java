package joomidang.papersummary.visualcontent.repository;

import java.util.List;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.visualcontent.entity.VisualContent;
import joomidang.papersummary.visualcontent.entity.VisualContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisualContentRepository extends JpaRepository<VisualContent, Long> {
    List<VisualContent> paperId(Long paperId);

    List<VisualContent> findByPaperIdAndSummaryIsNull(Long summaryId);

    List<VisualContent> findBySummaryAndType(Summary summary, VisualContentType type);
}
