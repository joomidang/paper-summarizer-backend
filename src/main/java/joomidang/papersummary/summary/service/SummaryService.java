package joomidang.papersummary.summary.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import joomidang.papersummary.summary.controller.response.SummaryListResponse;
import joomidang.papersummary.summary.controller.response.SummaryPublishResponse;
import joomidang.papersummary.summary.controller.response.SummaryResponse;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.entity.SummaryStats;
import joomidang.papersummary.summary.entity.SummaryVersion;
import joomidang.papersummary.summary.exception.SummaryCreationFailedException;
import joomidang.papersummary.summary.exception.SummaryNotFoundException;
import joomidang.papersummary.summary.repository.SummaryRepository;
import joomidang.papersummary.summary.repository.SummaryStatsRepository;
import joomidang.papersummary.tag.service.TagService;
import joomidang.papersummary.visualcontent.entity.VisualContentType;
import joomidang.papersummary.visualcontent.service.VisualContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    private static final int MAX_SEARCH_RESULTS = 1000;
    private static final int MIN_SEARCH_TERM_LENGTH = 2;

    private final PaperService paperService;
    private final SummaryRepository summaryRepository;
    private final SummaryStatsRepository summaryStatsRepository;
    private final VisualContentService visualContentService;
    private final MemberService memberService;
    private final S3Service s3Service;
    private final SummaryVersionService summaryVersionService;
    private final SummaryLikeService summaryLikeService;
    private final StatsEventPublisher statsEventPublisher;
    private final TagService tagService;

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
        tagService.attachTagsToSummary(summary, request.tags());

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

        // 태그 저장
        tagService.attachTagsToSummary(summary, request.tags());

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

        //tag 사용 횟수 감소
        tagService.decreaseTagUsageForSummary(summary);

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
        List<String> tags = tagService.getTagNamesBySummary(summaryId);

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
        List<String> tags = tagService.getTagNamesBySummary(summaryId);

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
    @Transactional
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

    /**
     * 인기 요약본 목록을 페이징으로 조회 가중치 기반 인기도 점수로 정렬(좋아요, 댓글, 조회수)
     */
    public SummaryListResponse getPopularSummaries(Pageable pageable) {
        log.debug("인기 요약본 목록 조회 시작: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        // 발행된 요약본들을 인기도 순으로 조회
        Page<Summary> summariesPage = summaryRepository.findPopularSummariesByPublishStatus(
                PublishStatus.PUBLISHED, pageable);

        // 빈 결과 처리
        if (summariesPage.isEmpty()) {
            log.debug("조회된 인기 요약본이 없음");
            return SummaryListResponse.empty(pageable);
        }

        // 요약본 ID 목록 추출
        List<Long> summaryIds = summariesPage.getContent().stream()
                .map(Summary::getId)
                .toList();

        // 인기도 점수 계산
        Map<Long, Double> popularityScores = calculatePopularityScores(summaryIds);

        // PopularSummaryResponse로 변환
        List<SummaryResponse> popularSummaries = getPopularSummaries(summariesPage, popularityScores);

        // 페이지 정보를 포함한 응답 생성
        Page<SummaryResponse> responsePage = new PageImpl<>(
                popularSummaries,
                pageable,
                summariesPage.getTotalElements()
        );

        log.debug("인기 요약본 목록 조회 완료: 조회된 요약본 수={}, 전체 페이지={}",
                responsePage.getNumberOfElements(), responsePage.getTotalPages());

        return SummaryListResponse.from(responsePage);
    }

    /**
     * 요약본 검색
     */
    public SummaryListResponse searchSummaries(String searchTerm, Pageable pageable) {
        log.debug("요약본 검색 시작: searchTerm={}, page={}, size={}",
                searchTerm, pageable.getPageNumber(), pageable.getPageSize());

        // 검색어 유효성 검증
        validateSearchTerm(searchTerm);

        // 페이지 크기 제한
        Pageable limitedPageable = limitPageable(pageable);

        // 단계별 검색 실행
        Page<Summary> summariesPage = executeSearchStrategy(searchTerm, limitedPageable);

        // 빈 결과 처리
        if (summariesPage.isEmpty()) {
            log.debug("검색 결과 없음: searchTerm={}", searchTerm);
            return SummaryListResponse.empty(limitedPageable);
        }

        // 인기도 기반 정렬 및 응답 생성
        return createSearchResponse(summariesPage, limitedPageable, searchTerm);
    }

    /**
     * 단계별 검색 전략 실행
     */
    private Page<Summary> executeSearchStrategy(String searchTerm, Pageable pageable) {
        log.debug("검색 전략 실행 시작: searchTerm={}", searchTerm);

        // 1단계: 원본 검색어로 정확 검색
        Page<Summary> results = searchByExactTerm(searchTerm, pageable);
        if (!results.isEmpty()) {
            log.debug("1단계 검색 성공: 원본 검색어로 {}개 결과", results.getTotalElements());
            return results;
        }

        // 2단계: 공백 제거 검색
        results = searchByNoSpaceTerm(searchTerm, pageable);
        if (!results.isEmpty()) {
            log.debug("2단계 검색 성공: 공백 제거 검색어로 {}개 결과", results.getTotalElements());
            return results;
        }

        // 3단계: 단어별 개별 검색
        results = searchByIndividualWords(searchTerm, pageable);
        if (!results.isEmpty()) {
            log.debug("3단계 검색 성공: 단어별 검색으로 {}개 결과", results.getTotalElements());
            return results;
        }

        log.debug("모든 검색 전략 실패: searchTerm={}", searchTerm);
        return Page.empty(pageable);
    }

    /**
     * 1단계: 원본 검색어로 정확 검색
     */
    private Page<Summary> searchByExactTerm(String searchTerm, Pageable pageable) {
        String processedTerm = preprocessSearchTerm(searchTerm);
        return summaryRepository.findByTitleContainingIgnoreCaseAndPublishStatus(
                processedTerm, PublishStatus.PUBLISHED, pageable);
    }

    /**
     * 2단계: 공백 제거 검색 ("딥 러닝" → "딥러닝")
     */
    private Page<Summary> searchByNoSpaceTerm(String searchTerm, Pageable pageable) {
        String noSpaceTerm = searchTerm.replaceAll("\\s+", "");

        // 원본과 동일하면 스킵
        if (noSpaceTerm.equals(searchTerm.trim())) {
            return Page.empty(pageable);
        }

        log.debug("공백 제거 검색: {} → {}", searchTerm, noSpaceTerm);
        return summaryRepository.findByTitleContainingIgnoreCaseAndPublishStatus(
                noSpaceTerm, PublishStatus.PUBLISHED, pageable);
    }

    /**
     * 3단계: 단어별 개별 검색
     */
    private Page<Summary> searchByIndividualWords(String searchTerm, Pageable pageable) {
        String[] words = searchTerm.trim().split("\\s+");

        // 단어가 1개면 이미 1단계에서 검색했으므로 스킵
        if (words.length <= 1) {
            return Page.empty(pageable);
        }

        log.debug("단어별 검색: {}", Arrays.toString(words));

        // 각 단어별로 검색하여 결과 합치기
        Set<Summary> allResults = new LinkedHashSet<>(); // 순서 유지 및 중복 제거

        for (String word : words) {
            String trimmedWord = word.trim();
            if (trimmedWord.length() >= MIN_SEARCH_TERM_LENGTH) {
                Page<Summary> wordResults = summaryRepository.findByTitleContainingIgnoreCaseAndPublishStatus(
                        trimmedWord, PublishStatus.PUBLISHED, PageRequest.of(0, MAX_SEARCH_RESULTS));
                allResults.addAll(wordResults.getContent());
            }
        }

        List<Summary> resultList = new ArrayList<>(allResults);

        // 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), resultList.size());

        if (start >= resultList.size()) {
            return Page.empty(pageable);
        }

        List<Summary> pageContent = resultList.subList(start, end);
        return new PageImpl<>(pageContent, pageable, resultList.size());
    }

    /**
     * 검색 결과를 인기도 기반으로 정렬하여 응답 생성
     */
    private SummaryListResponse createSearchResponse(Page<Summary> summariesPage, Pageable pageable,
                                                     String searchTerm) {
        // 요약본 ID 목록 추출
        List<Long> summaryIds = summariesPage.getContent().stream()
                .map(Summary::getId)
                .toList();

        // 인기도 점수 계산
        Map<Long, Double> popularityScores = calculatePopularityScoresForSearch(summaryIds);

        // SummaryResponse로 변환
        List<SummaryResponse> searchResults = summariesPage.getContent().stream()
                .map(summary -> {
                    Double score = popularityScores.getOrDefault(summary.getId(), 0.0);
                    return SummaryResponse.from(summary, score);
                })
                .toList();

        // 인기도 순으로 정렬
        List<SummaryResponse> sortedResults = searchResults.stream()
                .sorted((a, b) -> Double.compare(b.popularityScore(), a.popularityScore()))
                .toList();

        // 페이지 정보를 포함한 응답 생성
        Page<SummaryResponse> responsePage = new PageImpl<>(
                sortedResults,
                pageable,
                summariesPage.getTotalElements()
        );

        log.debug("요약본 검색 완료: searchTerm={}, 검색된 요약본 수={}, 전체 페이지={}",
                searchTerm, responsePage.getNumberOfElements(), responsePage.getTotalPages());

        return SummaryListResponse.from(responsePage);
    }

    /**
     * 검색어 유효성 검증
     */
    private void validateSearchTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        String trimmedTerm = searchTerm.trim();
        if (trimmedTerm.length() < MIN_SEARCH_TERM_LENGTH) {
            throw new IllegalArgumentException("검색어는 최소 " + MIN_SEARCH_TERM_LENGTH + "자 이상이어야 합니다.");
        }
    }

    /**
     * 검색어 전처리
     */
    private String preprocessSearchTerm(String searchTerm) {
        // 앞뒤 공백 제거, 연속된 공백을 단일 공백으로 변환
        return searchTerm.trim().replaceAll("\\s+", " ");
    }

    /**
     * 페이지 크기 제한
     */
    private Pageable limitPageable(Pageable pageable) {
        int requestedSize = pageable.getPageSize();
        int limitedSize = Math.min(requestedSize, MAX_SEARCH_RESULTS);

        if (requestedSize != limitedSize) {
            log.debug("페이지 크기 제한 적용: 요청={}, 적용={}", requestedSize, limitedSize);
        }

        return PageRequest.of(
                pageable.getPageNumber(),
                limitedSize,
                pageable.getSort()
        );
    }

    /**
     * 검색된 요약본들의 인기도 점수 계산
     */
    private Map<Long, Double> calculatePopularityScoresForSearch(List<Long> summaryIds) {
        if (summaryIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Double> popularityScores = new HashMap<>();
        List<Object[]> scoreResults = summaryRepository.calculatePopularityScores(summaryIds);

        for (Object[] result : scoreResults) {
            Long summaryId = ((Number) result[0]).longValue();
            Double score = ((Number) result[1]).doubleValue();
            popularityScores.put(summaryId, score);
        }

        return popularityScores;
    }

    private List<SummaryResponse> getSearchResults(Page<Summary> summariesPage, Map<Long, Double> popularityScores) {
        return getPopularSummaries(summariesPage, popularityScores);
    }

    private Page<Summary> getByTitleContainingIgnoreCaseAndPublishStatus(String processedKeyword,
                                                                         Pageable limitedPageable) {
        return summaryRepository.findByTitleContainingIgnoreCaseAndPublishStatus(
                processedKeyword, PublishStatus.PUBLISHED, limitedPageable);
    }


    private List<SummaryResponse> getPopularSummaries(Page<Summary> summariesPage, Map<Long, Double> popularityScores) {
        return summariesPage.getContent().stream()
                .map(summary -> {
                    Double score = popularityScores.getOrDefault(summary.getId(), 0.0);
                    return SummaryResponse.from(summary, score);
                })
                .toList();
    }

    private void validateS3Key(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            log.error("S3 키가 유효하지 않음: s3Key={}", s3Key);
            throw new SummaryCreationFailedException("S3 키가 유효하지 않습니다.");
        }
    }

    private void validateKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        String trimmedTerm = keyword.trim();
        if (trimmedTerm.length() < MIN_SEARCH_TERM_LENGTH) {
            throw new IllegalArgumentException("검색어는 최소 " + MIN_SEARCH_TERM_LENGTH + "자 이상이어야 합니다.");
        }
    }

    private String preprocessKeyword(String keyword) {
        return keyword.trim();
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

    private Map<Long, Double> calculatePopularityScores(List<Long> summaryIds) {
        if (summaryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Double> popularityScores = new HashMap<>();
        List<Object[]> scoreResults = summaryRepository.calculatePopularityScores(summaryIds);

        for (Object[] result : scoreResults) {
            Long summaryId = ((Number) result[0]).longValue();
            Double score = ((Number) result[1]).doubleValue();
            popularityScores.put(summaryId, score);
        }

        return popularityScores;
    }
}
