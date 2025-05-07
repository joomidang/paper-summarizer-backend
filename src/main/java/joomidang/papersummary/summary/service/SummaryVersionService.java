package joomidang.papersummary.summary.service;

import java.util.Optional;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.entity.SummaryVersion;
import joomidang.papersummary.summary.entity.VersionType;
import joomidang.papersummary.summary.repository.SummaryVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SummaryVersionService {
    private final SummaryVersionRepository summaryVersionRepository;

    public void createDraftVersion(Summary summary, String s3Key, String title, Member member) {
        int revision = summaryVersionRepository.findMaxRevision(summary.getId()).orElse(0) + 1;

        SummaryVersion version = SummaryVersion.builder()
                .summary(summary)
                .title(title)
                .s3KeyMd(s3Key)
                .versionType(VersionType.DRAFT)
                .revisionNumber(revision)
                .member(member)
                .build();

        summaryVersionRepository.save(version);
    }

    public void createPublishedVersion(Summary summary, String s3Key, String title, Member member) {
        int revision = summaryVersionRepository.findMaxRevision(summary.getId()).orElse(0) + 1;

        SummaryVersion version = SummaryVersion.builder()
                .summary(summary)
                .title(title)
                .s3KeyMd(s3Key)
                .versionType(VersionType.PUBLISHED)
                .revisionNumber(revision)
                .member(member)
                .build();

        summaryVersionRepository.save(version);
    }

    public Optional<SummaryVersion> findLatestDraft(Long summaryId) {
        return summaryVersionRepository.findTopBySummaryIdAndVersionTypeOrderByCreatedAtDesc(summaryId,
                VersionType.DRAFT);
    }
}
