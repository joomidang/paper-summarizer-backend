package joomidang.papersummary.summary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import joomidang.papersummary.common.config.rabbitmq.StatsEventPublisher;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.exception.MemberNotFoundException;
import joomidang.papersummary.member.service.MemberService;
import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.paper.exception.AccessDeniedException;
import joomidang.papersummary.paper.service.PaperService;
import joomidang.papersummary.s3.service.S3Service;
import joomidang.papersummary.summary.controller.request.SummaryEditRequest;
import joomidang.papersummary.summary.controller.response.LikedSummaryListResponse;
import joomidang.papersummary.summary.controller.response.LikedSummaryResponse;
import joomidang.papersummary.summary.controller.response.SummaryDetailResponse;
import joomidang.papersummary.summary.controller.response.SummaryEditDetailResponse;
import joomidang.papersummary.summary.controller.response.SummaryEditResponse;
import joomidang.papersummary.summary.controller.response.SummaryPublishResponse;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.entity.SummaryLike;
import joomidang.papersummary.summary.entity.SummaryStats;
import joomidang.papersummary.summary.entity.SummaryVersion;
import joomidang.papersummary.summary.exception.SummaryCreationFailedException;
import joomidang.papersummary.summary.exception.SummaryNotFoundException;
import joomidang.papersummary.summary.repository.SummaryLikeRepository;
import joomidang.papersummary.summary.repository.SummaryRepository;
import joomidang.papersummary.summary.repository.SummaryStatsRepository;
import joomidang.papersummary.visualcontent.entity.VisualContentType;
import joomidang.papersummary.visualcontent.service.VisualContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class SummaryServiceTest {

    private SummaryService summaryService;
    private PaperService paperService;
    private SummaryRepository summaryRepository;
    private SummaryStatsRepository summaryStatsRepository;
    private SummaryLikeRepository summaryLikeRepository;
    private VisualContentService visualContentService;
    private MemberService memberService;
    private S3Service s3Service;
    private SummaryVersionService summaryVersionService;
    private StatsEventPublisher statsEventPublisher;

    @BeforeEach
    void setUp() {
        paperService = mock(PaperService.class);
        summaryRepository = mock(SummaryRepository.class);
        summaryStatsRepository = mock(SummaryStatsRepository.class);
        summaryLikeRepository = mock(SummaryLikeRepository.class);
        visualContentService = mock(VisualContentService.class);
        memberService = mock(MemberService.class);
        s3Service = mock(S3Service.class);
        summaryVersionService = mock(SummaryVersionService.class);
        statsEventPublisher = mock(StatsEventPublisher.class);

        summaryService = new SummaryService(
                paperService,
                summaryRepository,
                summaryStatsRepository,
                summaryLikeRepository,
                visualContentService,
                memberService,
                s3Service,
                summaryVersionService,
                statsEventPublisher
        );
    }

    @Test
    @DisplayName("S3에서 요약 생성 성공 테스트")
    void createSummaryWithStatsFromS3Success() {
        //given
        Long paperId = 1L;
        String s3Key = "test-s3-key.md";

        //when
        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);

        Paper mockPaper = mock(Paper.class);
        when(mockPaper.getId()).thenReturn(paperId);
        when(mockPaper.getTitle()).thenReturn("Test Paper Title");
        when(mockPaper.getMember()).thenReturn(mockMember);
        when(paperService.findById(paperId)).thenReturn(mockPaper);

        when(summaryRepository.existsByPaper(mockPaper)).thenReturn(false);

        Summary mockSummary = Summary.builder()
                .id(1L)
                .title("Test Paper Title")
                .s3KeyMd(s3Key)
                .publishStatus(PublishStatus.DRAFT)
                .paper(mockPaper)
                .member(mockMember)
                .build();

        SummaryStats mockStats = SummaryStats.builder()
                .id(1L)
                .summary(mockSummary)
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .updatedAt(LocalDateTime.now())
                .build();

        when(summaryRepository.save(any(Summary.class))).thenReturn(mockSummary);
        when(summaryStatsRepository.save(any(SummaryStats.class))).thenReturn(mockStats);

        summaryService.createSummaryFromS3(paperId, s3Key);

        //then
        verify(paperService, times(1)).findById(paperId);
        verify(summaryRepository, times(1)).existsByPaper(mockPaper);
        verify(summaryStatsRepository, times(1)).save(any(SummaryStats.class));
        verify(visualContentService, times(1)).connectToSummary(any(Summary.class));
        verify(summaryRepository, times(2)).save(any(Summary.class));
    }

    @Test
    @DisplayName("S3 키가 유효하지 않을 때 예외 발생 테스트")
    void createSummaryWithStatsFromS3InvalidS3Key() {
        // given
        Long paperId = 1L;
        String s3Key = "";

        // when & then
        assertThrows(SummaryCreationFailedException.class, () -> {
            summaryService.createSummaryFromS3(paperId, s3Key);
        });

        // Verify no interactions with other services
        verify(paperService, times(0)).findById(anyLong());
        verify(summaryRepository, times(0)).existsByPaper(any(Paper.class));
        verify(visualContentService, times(0)).connectToSummary(any(Summary.class));
        verify(summaryRepository, times(0)).save(any(Summary.class));
    }

    @Test
    @DisplayName("이미 요약이 존재할 때 예외 발생 테스트")
    void createSummaryFromS3ExistingSummaryWithStats() {
        //given
        Long paperId = 1L;
        String s3Key = "test-s3-key.md";

        //when
        Paper mockPaper = mock(Paper.class);
        when(mockPaper.getId()).thenReturn(paperId);
        when(paperService.findById(paperId)).thenReturn(mockPaper);

        when(summaryRepository.existsByPaper(mockPaper)).thenReturn(true);

        //then
        assertThrows(SummaryCreationFailedException.class, () -> {
            summaryService.createSummaryFromS3(paperId, s3Key);
        });

        verify(paperService, times(1)).findById(paperId);
        verify(summaryRepository, times(1)).existsByPaper(mockPaper);
        verify(visualContentService, times(0)).connectToSummary(any(Summary.class));
        verify(summaryRepository, times(0)).save(any(Summary.class));
    }

    @Test
    @DisplayName("ID로 요약 findByIdWithStats 성공 테스트")
    void findByIdWithStatsSuccess() {
        //given
        Long summaryId = 1L;
        Summary mockSummary = mock(Summary.class);
        //when
        when(mockSummary.getId()).thenReturn(summaryId);
        when(summaryRepository.findByIdWithStats(summaryId)).thenReturn(Optional.of(mockSummary));

        Summary result = summaryService.findByIdWithStats(summaryId);

        //then
        assertNotNull(result);
        assertEquals(summaryId, result.getId());
        verify(summaryRepository, times(1)).findByIdWithStats(summaryId);
    }

    @Test
    @DisplayName("ID로 요약 findByIdWithoutStats NotFound 테스트")
    void findByIdWithoutStatsNotFound() {
        //given
        Long summaryId = 1L;
        //when
        when(summaryRepository.findByIdWithoutStats(summaryId)).thenReturn(Optional.empty());

        //then
        assertThrows(SummaryNotFoundException.class, () -> {
            summaryService.findByIdWithoutStats(summaryId);
        });
        verify(summaryRepository, times(1)).findByIdWithoutStats(summaryId);
    }

    @Test
    @DisplayName("요약본 편집을 위한 상세 정보 조회 성공 테스트")
    void getSummaryForEditSuccess() {
        //given
        String providerUid = "test-provider-uid";
        Long summaryId = 1L;

        //when
        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(summaryId);
        when(mockSummary.getTitle()).thenReturn("Test Summary Title");
        when(mockSummary.getBrief()).thenReturn("Test Brief");
        when(mockSummary.getS3KeyMd()).thenReturn("original-s3-key.md");
        when(mockSummary.isNotSameMemberId(1L)).thenReturn(false);
        when(summaryRepository.findByIdWithoutStats(summaryId)).thenReturn(Optional.of(mockSummary));

        when(summaryVersionService.findLatestDraft(summaryId)).thenReturn(Optional.empty());

        List<String> figureUrls = Arrays.asList("figure1.jpg", "figure2.jpg");
        List<String> tableUrls = Arrays.asList("table1.jpg");
        when(visualContentService.findUrlsBySummaryAndType(eq(mockSummary), eq(VisualContentType.FIGURE)))
                .thenReturn(figureUrls);
        when(visualContentService.findUrlsBySummaryAndType(eq(mockSummary), eq(VisualContentType.TABLE)))
                .thenReturn(tableUrls);

        SummaryEditDetailResponse response = summaryService.getSummaryForEdit(providerUid, summaryId);

        //then
        assertNotNull(response);
        assertEquals(mockSummary.getTitle(), response.getTitle());
        assertEquals(mockSummary.getBrief(), response.getBrief());
        assertEquals("https://paper-dev-test-magic-pdf-output.s3.ap-northeast-2.amazonaws.com/original-s3-key.md",
                response.getMarkdownUrl());
        assertEquals(figureUrls, response.getFigures());
        assertEquals(tableUrls, response.getTables());

        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(summaryRepository, times(1)).findByIdWithoutStats(summaryId);
        verify(summaryVersionService, times(1)).findLatestDraft(summaryId);
        verify(visualContentService, times(1)).findUrlsBySummaryAndType(eq(mockSummary), eq(VisualContentType.FIGURE));
    }

    @Test
    @DisplayName("요약본 편집을 위한 상세 정보 조회 시 최신 드래프트 버전 사용 테스트")
    void getSummaryForEditWithLatestDraft() {
        //given
        String providerUid = "test-provider-uid";
        Long summaryId = 1L;

        //when
        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(summaryId);
        when(mockSummary.getTitle()).thenReturn("Test Summary Title");
        when(mockSummary.getBrief()).thenReturn("Test Brief");
        when(mockSummary.getS3KeyMd()).thenReturn("original-s3-key.md");
        when(mockSummary.isNotSameMemberId(1L)).thenReturn(false);
        when(summaryRepository.findByIdWithoutStats(summaryId)).thenReturn(Optional.of(mockSummary));

        SummaryVersion mockVersion = mock(SummaryVersion.class);
        when(mockVersion.getS3KeyMd()).thenReturn("draft-s3-key.md");
        when(summaryVersionService.findLatestDraft(summaryId)).thenReturn(Optional.of(mockVersion));

        List<String> figureUrls = Arrays.asList("figure1.jpg", "figure2.jpg");
        List<String> tableUrls = Arrays.asList("table1.jpg");
        when(visualContentService.findUrlsBySummaryAndType(eq(mockSummary), eq(VisualContentType.FIGURE)))
                .thenReturn(figureUrls);
        when(visualContentService.findUrlsBySummaryAndType(eq(mockSummary), eq(VisualContentType.TABLE)))
                .thenReturn(tableUrls);

        SummaryEditDetailResponse response = summaryService.getSummaryForEdit(providerUid, summaryId);

        //then
        assertNotNull(response);
        assertEquals("https://paper-dev-test-magic-pdf-output.s3.ap-northeast-2.amazonaws.com/draft-s3-key.md",
                response.getMarkdownUrl());

        verify(summaryRepository, times(1)).findByIdWithoutStats(summaryId);
        verify(summaryVersionService, times(1)).findLatestDraft(summaryId);
    }

    @Test
    @DisplayName("권한 없는 사용자의 요약본 편집 정보 조회 시 예외 발생 테스트")
    void getSummaryForEditAccessDenied() {
        //given
        String providerUid = "test-provider-uid";
        Long summaryId = 1L;

        //when
        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(summaryId);
        when(mockSummary.isNotSameMemberId(1L)).thenReturn(true);
        when(summaryRepository.findByIdWithoutStats(summaryId)).thenReturn(Optional.of(mockSummary));

        //then
        assertThrows(AccessDeniedException.class, () -> {
            summaryService.getSummaryForEdit(providerUid, summaryId);
        });

        verify(summaryRepository, times(1)).findByIdWithoutStats(summaryId);
    }

    @Test
    @DisplayName("요약본 편집 내용 저장 성공 테스트")
    void saveSummaryEditSuccess() {
        //given
        String providerUid = "test-provider-uid";
        Long summaryId = 1L;
        SummaryEditRequest request = new SummaryEditRequest(
                "Updated Title",
                "Updated Brief",
                "Updated markdown content",
                Collections.emptyList()
        );

        //when
        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(summaryId);
        when(mockSummary.getPublishStatus()).thenReturn(PublishStatus.DRAFT);
        when(mockSummary.isNotSameMemberId(1L)).thenReturn(false);
        when(summaryRepository.findByIdWithoutStats(summaryId)).thenReturn(Optional.of(mockSummary));

        String markdownUrl = "https://.../draft-123.md";
        when(s3Service.saveMarkdownToS3(anyString(), eq(request.markdownContent()))).thenReturn(markdownUrl);

        SummaryEditResponse response = summaryService.saveSummaryEdit(providerUid, summaryId, request);

        //then
        assertNotNull(response);
        assertEquals(summaryId, response.getSummaryId());
        assertEquals(PublishStatus.DRAFT, response.getStatus());
        assertEquals(markdownUrl, response.getMarkdownUrl());

        verify(summaryRepository, times(1)).findByIdWithoutStats(summaryId);
        verify(summaryVersionService, times(1)).createDraftVersion(eq(mockSummary), anyString(), eq(request.title()),
                eq(mockMember));
    }


    @Test
    @DisplayName("요약본 발행 성공 테스트")
    void publishSummarySuccess() {
        //given
        String providerUid = "test-provider-uid";
        Long summaryId = 1L;
        SummaryEditRequest request = new SummaryEditRequest(
                "Published Title",
                "Published Brief",
                "Published markdown content",
                Collections.emptyList()
        );

        //when
        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(summaryId);
        when(mockSummary.getTitle()).thenReturn("Published Title");
        when(mockSummary.getUpdatedAt()).thenReturn(LocalDateTime.now());
        when(mockSummary.isNotSameMemberId(1L)).thenReturn(false);
        when(summaryRepository.findByIdWithoutStats(summaryId)).thenReturn(Optional.of(mockSummary));
        when(summaryRepository.save(mockSummary)).thenReturn(mockSummary);

        String markdownUrl = "https://.../publish-123.md";
        when(s3Service.saveMarkdownToS3(anyString(), eq(request.markdownContent()))).thenReturn(markdownUrl);

        SummaryPublishResponse response = summaryService.publishSummary(providerUid, summaryId, request);

        //then
        assertNotNull(response);
        assertEquals(summaryId, response.summaryId());
        assertEquals(markdownUrl, response.markdownUrl());
        assertEquals("Published Title", response.title());

        verify(summaryRepository, times(1)).findByIdWithoutStats(summaryId);
        verify(summaryVersionService, times(1)).createPublishedVersion(eq(mockSummary), anyString(),
                eq(request.title()), eq(mockMember));
        verify(mockSummary, times(1)).publish(eq(request.title()), eq(request.brief()), anyString());
        verify(summaryRepository, times(1)).save(mockSummary);
    }

    @Test
    @DisplayName("발행된 요약본 상세 조회 성공 테스트")
    void getSummaryDetailSuccess() {
        //given
        Long summaryId = 1L;
        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(summaryId);
        when(mockSummary.getPublishStatus()).thenReturn(PublishStatus.PUBLISHED);
        when(mockSummary.getS3KeyMd()).thenReturn("test-key.md");
        when(mockSummary.getUpdatedAt()).thenReturn(LocalDateTime.now());
        when(mockSummary.getViewCount()).thenReturn(10);
        when(mockSummary.getLikeCount()).thenReturn(5);
        when(summaryRepository.findByIdWithStats(summaryId)).thenReturn(Optional.of(mockSummary));

        SummaryDetailResponse response = summaryService.getSummaryDetail(summaryId);

        assertNotNull(response);
        assertEquals(summaryId, response.summaryId());
        verify(summaryRepository, times(1)).findByIdWithStats(summaryId);
    }

    @Test
    @DisplayName("발행되지 않은 요약본 상세 조회 시 예외 발생 테스트")
    void getSummaryDetailAccessDenied() {
        // given
        Long summaryId = 1L;

        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(summaryId);
        when(mockSummary.getPublishStatus()).thenReturn(PublishStatus.DRAFT); // Not published

        when(summaryRepository.findByIdWithStats(summaryId)).thenReturn(Optional.of(mockSummary));

        // when & then
        assertThrows(AccessDeniedException.class, () -> {
            summaryService.getSummaryDetail(summaryId);
        });

        verify(summaryRepository, times(1)).findByIdWithStats(summaryId);
    }

    @Test
    @DisplayName("요약본 삭제 성공 테스트")
    void deleteSummarySuccess() {
        String providerUid = "test-provider-uid";
        Long summaryId = 1L;
        Member mockMember = mock(Member.class);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);
        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(summaryId);
        when(mockMember.getId()).thenReturn(1L);
        when(mockSummary.isNotSameMemberId(mockMember.getId())).thenReturn(false);
        when(summaryRepository.findByIdWithoutStats(summaryId)).thenReturn(Optional.of(mockSummary));
        when(summaryRepository.save(mockSummary)).thenReturn(mockSummary);

        summaryService.deleteSummary(providerUid, summaryId);

        verify(summaryRepository, times(1)).findByIdWithoutStats(summaryId);
        verify(summaryVersionService, times(1)).deleteAllVersionBySummary(mockSummary);
        verify(mockSummary, times(1)).softDelete();
        verify(summaryRepository, times(1)).save(mockSummary);
    }

    @Test
    @DisplayName("요약본 삭제 시 SummaryStats도 함께 삭제 테스트")
    void deleteSummaryAlsoDeletesStats() {
        String providerUid = "uid";
        Long summaryId = 1L;

        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(memberService.findByProviderUid(providerUid)).thenReturn(member);

        Summary summary = mock(Summary.class);
        when(summary.isNotSameMemberId(1L)).thenReturn(false);
        when(summaryRepository.findByIdWithoutStats(summaryId)).thenReturn(Optional.of(summary));

        // when
        summaryService.deleteSummary(providerUid, summaryId);

        // then
        verify(summaryStatsRepository, times(1)).deleteBySummaryId(summaryId);
        verify(summaryVersionService, times(1)).deleteAllVersionBySummary(summary);
        verify(summary, times(1)).softDelete();
        verify(summaryRepository, times(1)).save(summary);
    }

    @Test
    @DisplayName("권한 없는 사용자의 요약본 삭제 시 예외 발생 테스트")
    void deleteSummaryAccessDenied() {
        String providerUid = "test-provider-uid";
        Long summaryId = 1L;

        // Stub member lookup
        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        Summary mockSummary = mock(Summary.class);
        when(mockSummary.isNotSameMemberId(1L)).thenReturn(true);
        when(summaryRepository.findByIdWithoutStats(summaryId)).thenReturn(Optional.of(mockSummary));

        assertThrows(AccessDeniedException.class, () -> summaryService.deleteSummary(providerUid, summaryId));
        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(summaryRepository, times(1)).findByIdWithoutStats(summaryId);
    }

    @Test
    @DisplayName("내가 좋아요한 요약본 목록 조회 성공 테스트")
    void getLikedSummariesSuccess() {
        // given
        String providerUid = "test-provider-uid";
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        // Mock SummaryLike 객체들 생성
        SummaryLike mockSummaryLike1 = createMockSummaryLike(1L, "첫 번째 요약본", mockMember);
        SummaryLike mockSummaryLike2 = createMockSummaryLike(2L, "두 번째 요약본", mockMember);
        List<SummaryLike> summaryLikes = Arrays.asList(mockSummaryLike1, mockSummaryLike2);

        Page<SummaryLike> mockPage = new PageImpl<>(summaryLikes, pageable, 2);
        when(summaryLikeRepository.findByMemberIdWithSummary(
                eq(1L), eq(PublishStatus.PUBLISHED), eq(pageable)))
                .thenReturn(mockPage);

        // when
        LikedSummaryListResponse response = summaryService.getLikedSummaries(providerUid, pageable);

        // then
        assertNotNull(response);
        assertEquals(2, response.summaries().size());
        assertEquals(0, response.currentPage());
        assertEquals(1, response.totalPages());
        assertEquals(2L, response.totalElements());
        assertFalse(response.hasNext());
        assertFalse(response.hasPrevious());

        // 첫 번째 요약본 검증
        LikedSummaryResponse firstSummary = response.summaries().get(0);
        assertEquals(1L, firstSummary.summaryId());
        assertEquals("첫 번째 요약본", firstSummary.title());

        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(summaryLikeRepository, times(1)).findByMemberIdWithSummary(
                eq(1L), eq(PublishStatus.PUBLISHED), eq(pageable));
    }

    @Test
    @DisplayName("좋아요한 요약본이 없는 경우 빈 목록 반환 테스트")
    void getLikedSummariesEmptyResult() {
        // given
        String providerUid = "test-provider-uid";
        Pageable pageable = PageRequest.of(0, 20);

        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        Page<SummaryLike> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(summaryLikeRepository.findByMemberIdWithSummary(
                eq(1L), eq(PublishStatus.PUBLISHED), eq(pageable)))
                .thenReturn(emptyPage);

        // when
        LikedSummaryListResponse response = summaryService.getLikedSummaries(providerUid, pageable);

        // then
        assertNotNull(response);
        assertEquals(0, response.summaries().size());
        assertEquals(0, response.currentPage());
        assertEquals(0, response.totalPages());
        assertEquals(0L, response.totalElements());
        assertFalse(response.hasNext());
        assertFalse(response.hasPrevious());

        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(summaryLikeRepository, times(1)).findByMemberIdWithSummary(
                eq(1L), eq(PublishStatus.PUBLISHED), eq(pageable));
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 좋아요한 요약본 조회 시 예외 발생 테스트")
    void getLikedSummariesMemberNotFound() {
        // given
        String providerUid = "non-existent-uid";
        Pageable pageable = PageRequest.of(0, 20);

        when(memberService.findByProviderUid(providerUid))
                .thenThrow(new MemberNotFoundException("사용자를 찾을 수 없습니다."));

        // when & then
        assertThrows(MemberNotFoundException.class, () -> {
            summaryService.getLikedSummaries(providerUid, pageable);
        });

        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(summaryLikeRepository, times(0)).findByMemberIdWithSummary(
                anyLong(), any(PublishStatus.class), any(Pageable.class));
    }

    @Test
    @DisplayName("페이징이 올바르게 동작하는지 테스트")
    void getLikedSummariesPagingTest() {
        // given
        String providerUid = "test-provider-uid";
        Pageable pageable = PageRequest.of(1, 10, Sort.by("createdAt").descending()); // 두 번째 페이지

        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        // 총 25개 중 두 번째 페이지(10개)
        List<SummaryLike> summaryLikes = createMockSummaryLikeList(10, mockMember);
        Page<SummaryLike> mockPage = new PageImpl<>(summaryLikes, pageable, 25);

        when(summaryLikeRepository.findByMemberIdWithSummary(
                eq(1L), eq(PublishStatus.PUBLISHED), eq(pageable)))
                .thenReturn(mockPage);

        // when
        LikedSummaryListResponse response = summaryService.getLikedSummaries(providerUid, pageable);

        // then
        assertNotNull(response);
        assertEquals(10, response.summaries().size());
        assertEquals(1, response.currentPage()); // 두 번째 페이지
        assertEquals(3, response.totalPages()); // 총 3페이지 (25개 / 10개)
        assertEquals(25L, response.totalElements());
        assertTrue(response.hasNext()); // 다음 페이지 있음
        assertTrue(response.hasPrevious()); // 이전 페이지 있음

        verify(summaryLikeRepository, times(1)).findByMemberIdWithSummary(
                eq(1L), eq(PublishStatus.PUBLISHED), eq(pageable));
    }

    @Test
    @DisplayName("발행된 요약본만 조회되는지 테스트")
    void getLikedSummariesOnlyPublishedSummaries() {
        // given
        String providerUid = "test-provider-uid";
        Pageable pageable = PageRequest.of(0, 20);

        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);
        Page<SummaryLike> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(summaryLikeRepository.findByMemberIdWithSummary(
                eq(1L), eq(PublishStatus.PUBLISHED), eq(pageable)))
                .thenReturn(emptyPage);
        // when
        summaryService.getLikedSummaries(providerUid, pageable);

        // then - PUBLISHED 상태만 조회하는지 확인
        verify(summaryLikeRepository, times(1)).findByMemberIdWithSummary(
                eq(1L), eq(PublishStatus.PUBLISHED), eq(pageable));
    }

    // Helper 메서드들
    private SummaryLike createMockSummaryLike(Long summaryId, String title, Member member) {
        SummaryLike mockSummaryLike = mock(SummaryLike.class);
        Summary mockSummary = mock(Summary.class);
        Member mockAuthor = mock(Member.class);

        when(mockSummary.getId()).thenReturn(summaryId);
        when(mockSummary.getTitle()).thenReturn(title);
        when(mockSummary.getBrief()).thenReturn(title + " 요약 내용");
        when(mockSummary.getMember()).thenReturn(mockAuthor);
        when(mockSummary.getUpdatedAt()).thenReturn(LocalDateTime.now());
        when(mockSummary.getViewCount()).thenReturn(10);
        when(mockSummary.getLikeCount()).thenReturn(5);
        when(mockSummary.getCommentCount()).thenReturn(3);

        when(mockAuthor.getName()).thenReturn("작성자" + summaryId);

        when(mockSummaryLike.getSummary()).thenReturn(mockSummary);
        when(mockSummaryLike.getCreatedAt()).thenReturn(LocalDateTime.now());

        return mockSummaryLike;
    }

    private List<SummaryLike> createMockSummaryLikeList(int count, Member member) {
        List<SummaryLike> summaryLikes = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            summaryLikes.add(createMockSummaryLike((long) i, "요약본 " + i, member));
        }
        return summaryLikes;
    }
}
