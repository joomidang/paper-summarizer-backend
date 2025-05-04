package joomidang.papersummary.summary.repository;

import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.summary.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, Long> {
    boolean existsByPaper(Paper paper);
}
