package joomidang.papersummary.summary.repository;

import java.util.List;
import java.util.Optional;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.entity.SummaryVersion;
import joomidang.papersummary.summary.entity.VersionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SummaryVersionRepository extends JpaRepository<SummaryVersion, Long> {
    @Query("SELECT MAX(v.revisionNumber) FROM SummaryVersion v WHERE v.summary.id = :summaryId")
    Optional<Integer> findMaxRevision(@Param("summaryId") Long summaryId);

    Optional<SummaryVersion> findTopBySummaryIdAndVersionTypeOrderByCreatedAtDesc(Long summaryId,
                                                                                  VersionType versionType);

    List<SummaryVersion> findBySummary(Summary summary);

    List<SummaryVersion> findBySummaryAndVersionTypeOrderByCreatedAtDesc(Summary summary, VersionType versionType);
}
