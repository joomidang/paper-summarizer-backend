package joomidang.papersummary.analysislog.repository;

import joomidang.papersummary.analysislog.entity.AnalysisLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisLogRepository extends JpaRepository<AnalysisLog, Long> {
}
