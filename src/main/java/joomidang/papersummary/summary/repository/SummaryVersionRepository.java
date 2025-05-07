package joomidang.papersummary.summary.repository;

import java.util.Optional;
import joomidang.papersummary.summary.entity.SummaryVersion;
import joomidang.papersummary.summary.entity.VersionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SummaryVersionRepository extends JpaRepository<SummaryVersion, Long> {
    @Query("SELECT MAX(v.revisionNumber) FROM SummaryVersion v WHERE v.summary.id = :summaryId")
    Optional<Integer> findMaxRevision(Long id);

    Optional<SummaryVersion> findTopBySummaryIdAndVersionTypeOrderByCreatedAtDesc(Long summaryId,
                                                                                  VersionType versionType);
}
