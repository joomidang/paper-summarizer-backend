package joomidang.papersummary.summary.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import joomidang.papersummary.common.config.rabbitmq.StatsEventPublisher;
import joomidang.papersummary.common.config.rabbitmq.StatsType;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.service.MemberService;
import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.paper.exception.AccessDeniedException;
import joomidang.papersummary.paper.service.PaperService;
import joomidang.papersummary.s3.service.S3Service;
import joomidang.papersummary.summary.controller.request.SummaryEditRequest;
import joomidang.papersummary.summary.controller.response.LikedSummaryListResponse;
import joomidang.papersummary.summary.controller.response.SummaryDetailResponse;
import joomidang.papersummary.summary.controller.response.SummaryEditDetailResponse;
import joomidang.papersummary.summary.controller.response.SummaryEditResponse;
import joomidang.papersummary.summary.controller.response.SummaryLikeResponse;
import joomidang.papersummary.summary.controller.response.SummaryPublishResponse;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.entity.SummaryStats;
import joomidang.papersummary.summary.entity.SummaryVersion;
import joomidang.papersummary.summary.exception.SummaryCreationFailedException;
import joomidang.papersummary.summary.exception.SummaryNotFoundException;
import joomidang.papersummary.summary.repository.SummaryRepository;
import joomidang.papersummary.summary.repository.SummaryStatsRepository;
import joomidang.papersummary.visualcontent.entity.VisualContentType;
import joomidang.papersummary.visualcontent.service.VisualContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SummaryService {
    private static final String S3_BASE_URL = "https://paper-dev-test-magic-pdf-output.s3.ap-northeast-2.amazonaws.com/";
    private static final String SUMMARIES_PATH = "summaries/";

    private final PaperService paperService;
    private final SummaryRepository summaryRepository;
    private final SummaryStatsRepository summaryStatsRepository;
    private final VisualContentService visualContentService;
    private final MemberService memberService;
    private final S3Service s3Service;
    private final SummaryVersionService summaryVersionService;
    private final SummaryLikeService summaryLikeService;
    private final StatsEventPublisher statsEventPublisher;

    @Transactional
    public Long createSummaryFromS3(Long paperId, String s3Key) {
        log.info("S3에서 요약 생성 시작: paperId={}", paperId);
        log.debug("전달된 s3Key: {}", s3Key);

        validateS3Key(s3Key);
        Paper paper = findPaper(paperId);
        checkExistingSummary(paper);

        Summary summary = createSummaryWithStats(paper, s3Key);
        connectVisualsToSummary(summary);
        Summary savedSummary = saveSummary(summary);

        log.info("요약 저장 완료: paperId={}, summaryId={}", paperId, summary.getSummaryId());
        return savedSummary.getSummaryId();
    }

    /**
     * 요약본 편집 내용 저장
     */
    @Transactional
    public SummaryEditResponse saveSummaryEdit(String providerUid, Long summaryId, SummaryEditRequest request) {
        log.debug("요약본 편집 내용 저장 시작: summaryId={}", summaryId);
        Member member = memberService.findByProviderUid(providerUid);
        Summary summary = validateSummaryAccess(summaryId, member);

        // 요약본 정보 업데이트
        String key = generateS3Key(summaryId, "draft");
        String markdownUrl = s3Service.saveMarkdownToS3(key, request.markdownContent());

        summaryVersionService.createDraftVersion(summary, key, request.title(), member);

        // 태그 저장
        // TODO: 태그 저장 로직 구현 필요

        SummaryEditResponse response = SummaryEditResponse.of(
                summaryId,
                summary.getPublishStatus(),
                markdownUrl,
                LocalDateTime.now()
        );

        log.debug("요약본 편집 내용 저장 완료: summaryId={}", summaryId);
        return response;
    }

    /**
     * 요약본 업로드
     */
    @Transactional
    public SummaryPublishResponse publishSummary(String providerUid, Long summaryId, SummaryEditRequest request) {
        log.debug("요약본 발행 시작: summaryId={}", summaryId);
        Member member = memberService.findByProviderUid(providerUid);
        Summary summary = validateSummaryAccess(summaryId, member);

        // S3에 마크다운 저장
        String s3Key = generateS3Key(summaryId, "publish");
        String markdownUrl = s3Service.saveMarkdownToS3(s3Key, request.markdownContent());

        // summary version 저장
        summaryVersionService.createPublishedVersion(summary, s3Key, request.title(), member);

        // summary 테이블 업데이트
        summary.publish(
                request.title(),
                request.brief(),
                s3Key
        );
        saveSummary(summary);

        return SummaryPublishResponse.of(
                summary.getId(),
                summary.getTitle(),
                markdownUrl,
                summary.getUpdatedAt()
        );
    }

    /**
     * 요약본 삭제 - Summary는 소프트 삭제 (isDeleted=true, publishStatus=DELETED) - SummaryVersion은 모두 하드 삭제 (DRAFT, PUBLISHED 모두
     * 삭제) - S3에 저장된 파일도 함께 삭제
     */
    @Transactional
    public void deleteSummary(String providerUid, Long summaryId) {
        log.debug("요약본 삭제 시작: summaryId={}", summaryId);
        Member member = memberService.findByProviderUid(providerUid);
        Summary summary = validateSummaryAccess(summaryId, member);

        //통계 삭제
        summaryStatsRepository.deleteBySummaryId(summaryId);

        // 모든 버전 삭제 (S3에 저장되어있는 파일도 삭제)
        summaryVersionService.deleteAllVersionBySummary(summary);

        // Summary의 S3 파일 삭제
        String s3KeyMd = summary.getS3KeyMd();
        if (s3KeyMd != null && !s3KeyMd.isBlank()) {
            try {
                log.debug("요약본 S3 파일 삭제: {}", s3KeyMd);
                s3Service.deleteFile(s3KeyMd);
            } catch (Exception e) {
                log.error("요약본 S3 파일 삭제 중 오류 발생: summaryId={}, s3KeyMd={}, error={}",
                        summaryId, s3KeyMd, e.getMessage(), e);
                // 파일 삭제 실패해도 요약본 삭제는 계속 진행
            }
        }

        // 요약본 소프트 삭제
        summary.softDelete();
        summaryRepository.save(summary);
        log.debug("요약본 삭제 완료: summaryId={}", summaryId);
    }

    /**
     * 요약본 편집을 위한 상세 정보 조회
     */
    public SummaryEditDetailResponse getSummaryForEdit(String providerUid, Long summaryId) {
        log.debug("요약본 편집을 위한 상세 정보 조회 시작: summaryId={}", summaryId);
        Member member = memberService.findByProviderUid(providerUid);
        Summary summary = validateSummaryAccess(summaryId, member);

        //최신 DRAFT 버전이 있으면 사용
        String s3Key = summaryVersionService.findLatestDraft(summaryId)
                .map(SummaryVersion::getS3KeyMd)
                .orElse(summary.getS3KeyMd());

        // S3 키를 URL로 변환
        String markdownUrl = getMarkdownUrl(s3Key);

        // 태그 목록 조회
        // TODO: 태그 조회 로직 구현 필요
        List<String> tags = Collections.emptyList();

        // 시각 콘텐츠(figures, tables) 조회
        List<String> figures = visualContentService.findUrlsBySummaryAndType(summary, VisualContentType.FIGURE);
        List<String> tables = visualContentService.findUrlsBySummaryAndType(summary, VisualContentType.TABLE);
        log.debug("시각 콘텐츠 조회 완료: summaryId={}, figuresCount={}, tablesCount={}",
                summaryId, figures.size(), tables.size());

        SummaryEditDetailResponse response = SummaryEditDetailResponse.from(summary, markdownUrl, figures, tables,
                tags);
        log.debug("요약본 편집을 위한 상세 정보 조회 완료: summaryId={}", summaryId);

        return response;
    }

    /**
     * 요약본 단건 조회
     */
    public SummaryDetailResponse getSummaryDetail(Long summaryId) {
        Summary summary = findByIdWithStats(summaryId);

        if (summary.getPublishStatus() != PublishStatus.PUBLISHED) {
            throw new AccessDeniedException("발행되지 않은 요약본은 조회할 수 없습니다.");
        }

        String markdownUrl = getMarkdownUrl(summary.getS3KeyMd());

        // 태그 목록 조회
        // TODO: 태그 조회 로직 구현 필요
        List<String> tags = Collections.emptyList();

        statsEventPublisher.publish(summaryId, StatsType.VIEW);
        return SummaryDetailResponse.from(summary, markdownUrl, tags);
    }

    /**
     * 요약본 상태 정보 포함해서 조회
     */
    public Summary findByIdWithStats(Long summaryId) {
        log.debug("요약 정보 조회 시작: summaryId={}", summaryId);

        Summary summary = summaryRepository.findByIdWithStats(summaryId)
                .orElseThrow(() -> {
                    log.error("요약 정보를 찾을 수 없음: summaryId={}", summaryId);
                    return new SummaryNotFoundException(summaryId);
                });

        log.debug("요약 정보 조회 완료: summaryId={}", summaryId);
        return summary;
    }

    /**
     * 요약본 상태 정보 없이 조회
     */
    public Summary findByIdWithoutStats(Long summaryId) {
        return summaryRepository.findByIdWithoutStats(summaryId)
                .orElseThrow(() -> new SummaryNotFoundException(summaryId));
    }

    /**
     * 요약본 좋아요
     */
    public SummaryLikeResponse toggleLikeSummary(String providerUid, Long summaryId) {
        log.debug("요약본 좋아요 토글 시작: summaryId={}", summaryId);

        Summary summary = findByIdWithStats(summaryId);
        validateSummaryForLike(summary);

        int beforeCount = summary.getLikeCount();

        boolean isLiked = summaryLikeService.toggleLike(providerUid, summary);

        int updatedCount = isLiked ? beforeCount + 1 : beforeCount - 1;
        updatedCount = Math.max(0, updatedCount);

        log.debug("요약본 좋아요 토글 완료: summaryId={}, isLiked={}", summaryId, isLiked);
        return new SummaryLikeResponse(isLiked, updatedCount);
    }

    /**
     * 좋아요한 요약본 조회
     */
    public LikedSummaryListResponse getLikedSummaries(String providerUid, Pageable pageable) {
        return summaryLikeService.getLikedSummaries(providerUid, pageable);
    }

    private void validateS3Key(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            log.error("S3 키가 유효하지 않음: s3Key={}", s3Key);
            throw new SummaryCreationFailedException("S3 키가 유효하지 않습니다.");
        }
    }

    private Paper findPaper(Long paperId) {
        log.debug("논문 정보 조회 시작: paperId={}", paperId);
        Paper paper = paperService.findById(paperId);
        log.debug("논문 정보 조회 완료: paperId={}, title={}", paperId, paper.getTitle());
        return paper;
    }

    private void checkExistingSummary(Paper paper) {
        log.debug("기존 요약 존재 여부 확인: paperId={}", paper.getId());
        if (summaryRepository.existsByPaper(paper)) {
            log.warn("이미 요약이 존재합니다: paperId={}", paper.getId());
            // 기존 요약이 있으면 업데이트하는 로직을 추가할 수 있음
            throw new SummaryCreationFailedException("이미 요약이 존재합니다.");
        }
        log.debug("기존 요약 없음, 새 요약 생성 진행");
    }

    private Summary createSummaryWithStats(Paper paper, String s3Key) {
        log.debug("요약 엔티티 생성: title={}, s3KeyMd={}, memberId={}",
                paper.getTitle(), s3Key, paper.getMember().getId());
        Summary summary = getSummary(paper, s3Key);
        Summary savedSummary = summaryRepository.save(summary);
        SummaryStats summaryStats = SummaryStats.builder()
                .summary(savedSummary)
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .updatedAt(LocalDateTime.now())
                .build();
        savedSummary.setSummaryStats(summaryStats);
        summaryStatsRepository.save(summaryStats);
        return savedSummary;
    }

    private Summary saveSummary(Summary summary) {
        log.debug("요약본 저장 시작: summaryId={}", summary.getId());
        Summary savedSummary = summaryRepository.save(summary);
        log.debug("요약본 저장 완료: summaryId={}", savedSummary.getId());
        return savedSummary;
    }

    private void connectVisualsToSummary(Summary summary) {
        visualContentService.connectToSummary(summary);
    }

    private Summary validateSummaryAccess(Long summaryId, Member requester) {
        Summary summary = findByIdWithoutStats(summaryId);
        validateAccess(summary, requester);
        return summary;
    }

    private void validateAccess(Summary summary, Member requester) {
        if (summary.isNotSameMemberId(requester.getId())) {
            throw new AccessDeniedException();
        }
    }

    private void validateSummaryForLike(Summary summary) {
        if (summary.getPublishStatus() != PublishStatus.PUBLISHED) {
            throw new IllegalArgumentException("발행되지 않은 요약본에는 좋아요할 수 없습니다.");
        }

        if (summary.isDeleted()) {
            throw new IllegalArgumentException("삭제된 요약본에는 좋아요할 수 없습니다.");
        }
    }

    private String getMarkdownUrl(String s3Key) {
        return S3_BASE_URL + s3Key;
    }

    private String generateS3Key(Long summaryId, String prefix) {
        return SUMMARIES_PATH + summaryId + "/" + prefix + "-" + System.currentTimeMillis() + ".md";
    }

    private Summary getSummary(Paper paper, String s3Key) {
        return Summary.builder()
                .title(paper.getTitle())
                .s3KeyMd(s3Key)
                .publishStatus(PublishStatus.DRAFT)
                .paper(paper)
                .member(paper.getMember())
                .build();
    }

    private void publishLikeEvent(Long summaryId, String action) {
        if (action.equals("like")) {
            statsEventPublisher.publish(summaryId, StatsType.LIKE);
        } else if (action.equals("dislike")) {
            statsEventPublisher.publish(summaryId, StatsType.DISLIKE);
        } else {
            throw new IllegalArgumentException("action은 like 또는 dislike만 가능합니다.");
        }
    }
}
