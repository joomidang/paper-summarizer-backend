package joomidang.papersummary.visualcontent.repository;

import java.util.List;
import joomidang.papersummary.visualcontent.entity.VisualContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisualContentRepository extends JpaRepository<VisualContent, Long> {
    List<VisualContent> paperId(Long paperId);

    List<VisualContent> findByPaperIdAndSummaryIsNull(Long summaryId);
}
