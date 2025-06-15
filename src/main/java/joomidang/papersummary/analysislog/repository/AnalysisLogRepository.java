package joomidang.papersummary.analysislog.repository;

import java.util.Optional;
import joomidang.papersummary.analysislog.entity.AnalysisLog;
import joomidang.papersummary.analysislog.entity.AnalysisStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisLogRepository extends JpaRepository<AnalysisLog, Long> {
    Optional<AnalysisLog> findTopByPaperIdAndStageOrderByStartedAtDesc(Long paperId, AnalysisStage stage);
}
