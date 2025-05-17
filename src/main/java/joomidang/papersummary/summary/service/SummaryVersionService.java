package joomidang.papersummary.summary.service;

import java.util.List;
import java.util.Optional;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.s3.service.S3Service;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.entity.SummaryVersion;
import joomidang.papersummary.summary.entity.VersionType;
import joomidang.papersummary.summary.repository.SummaryVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SummaryVersionService {
    private final SummaryVersionRepository summaryVersionRepository;
    private final S3Service s3Service;

    private static final int MAX_DRAFT_VERSIONS = 5;

    /**
     * 임시 저장 버전을 생성합니다. 최대 5개의 DRAFT 버전만 유지하고 나머지는 삭제합니다.
     */
    public void createDraftVersion(Summary summary, String s3Key, String title, Member member) {
        log.debug("임시 저장 버전 생성 시작: summaryId={}, title={}", summary.getId(), title);

        int revision = summaryVersionRepository.findMaxRevision(summary.getId()).orElse(0) + 1;

        SummaryVersion version = buildSummaryVersion(summary, s3Key, title, member, revision);
        // 최대 5개의 DRAFT 버전만 유지하고 나머지는 삭제
        checkAndDeleteExcessDraftVersions(summary);

        log.debug("새 DRAFT 버전 생성 완료: versionId={}, revision={}", version.getId(), revision);
    }

    public void createPublishedVersion(Summary summary, String s3Key, String title, Member member) {
        log.debug("발행 버전 생성 시작: summaryId={}, title={}", summary.getId(), title);

        //기존 PUBLISHED 버전들을 DRAFT로 변경
        changeOldPublishVersionToDraft(summary);

        int revision = summaryVersionRepository.findMaxRevision(summary.getId()).orElse(0) + 1;

        SummaryVersion version = buildSummaryVersion(summary, s3Key, title, member, revision);

        log.debug("새 PUBLISHED 버전 생성 완료: versionId={}, revision={}", version.getId(), revision);
    }

    public Optional<SummaryVersion> findLatestDraft(Long summaryId) {
        return summaryVersionRepository.findTopBySummaryIdAndVersionTypeOrderByCreatedAtDesc(summaryId,
                VersionType.DRAFT);
    }

    /**
     * 요약본의 모든 버전을 삭제합니다. - 요약본 삭제 시: 모든 버전(DRAFT, PUBLISHED) 하드 삭제 - S3에 저장된 파일도 함께 삭제
     */
    public void deleteAllVersionBySummary(Summary summary) {
        log.debug("요약본 버전 삭제 시작: summaryId={}", summary.getId());

        // 1. DRAFT 버전 처리 - 모두 삭제
        List<SummaryVersion> draftVersions = summaryVersionRepository.findBySummaryAndVersionTypeOrderByCreatedAtDesc(
                summary, VersionType.DRAFT);
        log.debug("DRAFT 버전 삭제 대상: {}개", draftVersions.size());

        // S3 파일 삭제 후 버전 삭제
        deleteVersionsWithS3Files(draftVersions);

        // 2. PUBLISHED 버전 처리 - 모두 삭제
        List<SummaryVersion> publishedVersions = summaryVersionRepository.findBySummaryAndVersionTypeOrderByCreatedAtDesc(
                summary, VersionType.PUBLISHED);
        log.debug("PUBLISHED 버전 삭제 대상: {}개", publishedVersions.size());

        // S3 파일 삭제 후 버전 삭제
        deleteVersionsWithS3Files(publishedVersions);

        log.debug("요약본 버전 삭제 완료: summaryId={}", summary.getId());
    }

    private SummaryVersion buildSummaryVersion(Summary summary, String s3Key, String title, Member member,
                                               int revision) {
        SummaryVersion version = SummaryVersion.builder()
                .summary(summary)
                .title(title)
                .s3KeyMd(s3Key)
                .versionType(VersionType.DRAFT)
                .revisionNumber(revision)
                .member(member)
                .build();

        summaryVersionRepository.save(version);
        return version;
    }

    /**
     * 버전 목록을 받아 S3 파일을 삭제한 후 버전을 삭제합니다.
     */
    private void deleteVersionsWithS3Files(List<SummaryVersion> versions) {
        for (SummaryVersion version : versions) {
            try {
                // S3 파일 삭제
                String s3KeyMd = version.getS3KeyMd();
                if (s3KeyMd != null && !s3KeyMd.isBlank()) {
                    log.debug("S3 파일 삭제: {}", s3KeyMd);
                    s3Service.deleteFile(s3KeyMd);
                }

                // 버전 삭제
                log.debug("버전 삭제: versionId={}, type={}", version.getId(), version.getVersionType());
                summaryVersionRepository.delete(version);
            } catch (Exception e) {
                log.error("버전 삭제 중 오류 발생: versionId={}, error={}", version.getId(), e.getMessage(), e);
                // 오류가 발생해도 다른 버전 삭제 계속 진행
            }
        }
    }

    private void changeOldPublishVersionToDraft(Summary summary) {
        List<SummaryVersion> publishedVersions = summaryVersionRepository.findBySummaryAndVersionTypeOrderByCreatedAtDesc(
                summary, VersionType.PUBLISHED);

        if (!publishedVersions.isEmpty()) {
            log.debug("기존 PUBLISHED 버전을 DRAFT로 변경: {}개", publishedVersions.size());
            for (SummaryVersion version : publishedVersions) {
                version.changeVersionType(VersionType.DRAFT);
                summaryVersionRepository.save(version);
                log.debug("버전 상태 변경: versionId={}, PUBLISHED -> DRAFT", version.getId());
            }

            // DRAFT 버전 제한 체크 (선택사항)
            checkAndDeleteExcessDraftVersions(summary);
        }
    }

    private void checkAndDeleteExcessDraftVersions(Summary summary) {
        List<SummaryVersion> draftVersions = summaryVersionRepository.findBySummaryAndVersionTypeOrderByCreatedAtDesc(
                summary, VersionType.DRAFT);

        if (draftVersions.size() > MAX_DRAFT_VERSIONS) {
            List<SummaryVersion> draftVersionsToDelete = draftVersions.subList(MAX_DRAFT_VERSIONS,
                    draftVersions.size());
            log.debug("DRAFT 버전 제한으로 인한 삭제 대상: {}개", draftVersionsToDelete.size());

            // S3 파일 삭제 후 버전 삭제
            deleteVersionsWithS3Files(draftVersionsToDelete);
        }
    }
}
