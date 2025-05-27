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
import joomidang.papersummary.summary.controller.response.AuthorResponse;
import joomidang.papersummary.summary.controller.response.LikedSummaryListResponse;
import joomidang.papersummary.summary.controller.response.LikedSummaryResponse;
import joomidang.papersummary.summary.controller.response.PopularSummaryListResponse;
import joomidang.papersummary.summary.controller.response.PopularSummaryResponse;
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
    private VisualContentService visualContentService;
    private MemberService memberService;
    private S3Service s3Service;
    private SummaryVersionService summaryVersionService;
    private SummaryLikeService summaryLikeService;
    private StatsEventPublisher statsEventPublisher;

    @BeforeEach
    void setUp() {
        paperService = mock(PaperService.class);
        summaryRepository = mock(SummaryRepository.class);
        summaryStatsRepository = mock(SummaryStatsRepository.class);
        visualContentService = mock(VisualContentService.class);
        memberService = mock(MemberService.class);
        s3Service = mock(S3Service.class);
        summaryVersionService = mock(SummaryVersionService.class);
        summaryLikeService = mock(SummaryLikeService.class);
        statsEventPublisher = mock(StatsEventPublisher.class);

        summaryService = new SummaryService(
                paperService,
                summaryRepository,
                summaryStatsRepository,
                visualContentService,
                memberService,
                s3Service,
                summaryVersionService,
                summaryLikeService,
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
    @DisplayName("요약본 좋아요 토글 성공 테스트 - 좋아요 추가")
    void toggleLikeSummaryAddLike() {
        // given
        String providerUid = "test-provider-uid";
        Long summaryId = 1L;

        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(summaryId);
        when(mockSummary.getLikeCount()).thenReturn(0); // 좋아요 추가 전 카운트
        when(mockSummary.getPublishStatus()).thenReturn(PublishStatus.PUBLISHED);
        when(mockSummary.isDeleted()).thenReturn(false);
        when(summaryRepository.findByIdWithStats(summaryId)).thenReturn(Optional.of(mockSummary));

        // SummaryLikeService에서 좋아요 추가 반환
        when(summaryLikeService.toggleLike(providerUid, mockSummary)).thenReturn(true);

        // when
        SummaryLikeResponse response = summaryService.toggleLikeSummary(providerUid, summaryId);

        // then
        assertNotNull(response);
        assertTrue(response.liked());
        assertEquals(1, response.likeCount());

        verify(summaryRepository, times(1)).findByIdWithStats(summaryId);
        verify(summaryLikeService, times(1)).toggleLike(providerUid, mockSummary);
    }

    @Test
    @DisplayName("요약본 좋아요 토글 성공 테스트 - 좋아요 취소")
    void toggleLikeSummaryRemoveLike() {
        // given
        String providerUid = "test-provider-uid";
        Long summaryId = 1L;

        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(summaryId);
        when(mockSummary.getLikeCount()).thenReturn(1);
        when(mockSummary.getPublishStatus()).thenReturn(PublishStatus.PUBLISHED);
        when(mockSummary.isDeleted()).thenReturn(false);

        when(summaryRepository.findByIdWithStats(summaryId)).thenReturn(Optional.of(mockSummary));
        when(summaryLikeService.toggleLike(providerUid, mockSummary)).thenReturn(false);

        // when
        SummaryLikeResponse response = summaryService.toggleLikeSummary(providerUid, summaryId);

        // then
        assertNotNull(response);
        assertFalse(response.liked());
        assertEquals(0, response.likeCount());

        verify(summaryRepository, times(1)).findByIdWithStats(summaryId);
        verify(summaryLikeService, times(1)).toggleLike(providerUid, mockSummary);
    }

    @Test
    @DisplayName("내가 좋아요한 요약본 목록 조회 성공 테스트")
    void getLikedSummariesSuccess() {
        // given
        String providerUid = "test-provider-uid";
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

        // Create author responses
        AuthorResponse author1 = AuthorResponse.builder()
                .id(42L)
                .username("작성자1")
                .profileImageUrl("https://example.com/profiles/42.png")
                .build();

        AuthorResponse author2 = AuthorResponse.builder()
                .id(55L)
                .username("작성자2")
                .profileImageUrl("https://example.com/profiles/55.png")
                .build();

        // Create tags
        List<String> tags = Arrays.asList("GPT", "ML");

        // 간단한 응답 객체 생성 (from 메서드 테스트는 별도로)
        LikedSummaryResponse summary1 = new LikedSummaryResponse(
                1L, "첫 번째 요약본", author1,
                LocalDateTime.now(), LocalDateTime.now(), 5, tags
        );
        LikedSummaryResponse summary2 = new LikedSummaryResponse(
                2L, "두 번째 요약본", author2,
                LocalDateTime.now(), LocalDateTime.now(), 8, tags
        );

        List<LikedSummaryResponse> summaries = Arrays.asList(summary1, summary2);
        LikedSummaryListResponse mockResponse = new LikedSummaryListResponse(
                new LikedSummaryListResponse.ContentWrapper(summaries),
                1,  // page
                20,  // size
                2L, // totalElements
                1   // totalPages
        );

        when(summaryLikeService.getLikedSummaries(providerUid, pageable)).thenReturn(mockResponse);

        // when
        LikedSummaryListResponse response = summaryService.getLikedSummaries(providerUid, pageable);

        // then
        assertNotNull(response);
        assertEquals(2, response.content().content().size());
        assertEquals(1, response.page());
        assertEquals(1, response.totalPages());
        assertEquals(2L, response.totalElements());
        assertEquals(20, response.size());

        verify(summaryLikeService, times(1)).getLikedSummaries(providerUid, pageable);
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 좋아요한 요약본 조회 시 예외 발생 테스트")
    void getLikedSummariesMemberNotFound() {
        // given
        String providerUid = "non-existent-uid";
        Pageable pageable = PageRequest.of(0, 20);

        when(summaryLikeService.getLikedSummaries(providerUid, pageable))
                .thenThrow(new MemberNotFoundException("사용자를 찾을 수 없습니다."));

        // when & then
        assertThrows(MemberNotFoundException.class, () -> {
            summaryService.getLikedSummaries(providerUid, pageable);
        });

        verify(summaryLikeService, times(1)).getLikedSummaries(providerUid, pageable);
    }

    @Test
    @DisplayName("페이징이 올바르게 동작하는지 테스트")
    void getLikedSummariesPagingTest() {
        // given
        String providerUid = "test-provider-uid";
        Pageable pageable = PageRequest.of(1, 10, Sort.by("createdAt").descending()); // 두 번째 페이지

        // 총 25개 중 두 번째 페이지(10개) 응답 생성
        List<LikedSummaryResponse> summaries = createMockLikedSummaryResponseList(10);
        LikedSummaryListResponse mockResponse = new LikedSummaryListResponse(
                new LikedSummaryListResponse.ContentWrapper(summaries),
                2,    // page (1-based)
                10,   // size
                25L,  // totalElements
                3     // totalPages
        );

        when(summaryLikeService.getLikedSummaries(providerUid, pageable)).thenReturn(mockResponse);

        // when
        LikedSummaryListResponse response = summaryService.getLikedSummaries(providerUid, pageable);

        // then
        assertNotNull(response);
        assertEquals(10, response.content().content().size());
        assertEquals(2, response.page()); // 두 번째 페이지 (1-based)
        assertEquals(3, response.totalPages()); // 총 3페이지 (25개 / 10개)
        assertEquals(25L, response.totalElements());
        assertEquals(10, response.size());

        verify(summaryLikeService, times(1)).getLikedSummaries(providerUid, pageable);
    }

    @Test
    @DisplayName("인기 요약본 목록 조회 성공 테스트")
    void getPopularSummariesSuccess() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());

        // Mock Summary 객체들 생성
        Member mockMember1 = mock(Member.class);
        when(mockMember1.getName()).thenReturn("작성자1");
        when(mockMember1.getProfileImage()).thenReturn("profile1.jpg");

        Member mockMember2 = mock(Member.class);
        when(mockMember2.getName()).thenReturn("작성자2");
        when(mockMember2.getProfileImage()).thenReturn("profile2.jpg");

        Summary mockSummary1 = mock(Summary.class);
        when(mockSummary1.getId()).thenReturn(1L);
        when(mockSummary1.getTitle()).thenReturn("인기 요약본 1");
        when(mockSummary1.getBrief()).thenReturn("인기 요약본 1의 내용");
        when(mockSummary1.getMember()).thenReturn(mockMember1);
        when(mockSummary1.getUpdatedAt()).thenReturn(LocalDateTime.now());
        when(mockSummary1.getViewCount()).thenReturn(100);
        when(mockSummary1.getLikeCount()).thenReturn(20);
        when(mockSummary1.getCommentCount()).thenReturn(5);

        Summary mockSummary2 = mock(Summary.class);
        when(mockSummary2.getId()).thenReturn(2L);
        when(mockSummary2.getTitle()).thenReturn("인기 요약본 2");
        when(mockSummary2.getBrief()).thenReturn("인기 요약본 2의 내용");
        when(mockSummary2.getMember()).thenReturn(mockMember2);
        when(mockSummary2.getUpdatedAt()).thenReturn(LocalDateTime.now());
        when(mockSummary2.getViewCount()).thenReturn(80);
        when(mockSummary2.getLikeCount()).thenReturn(15);
        when(mockSummary2.getCommentCount()).thenReturn(3);

        List<Summary> summaries = Arrays.asList(mockSummary1, mockSummary2);
        Page<Summary> summariesPage = new PageImpl<>(summaries, pageable, 2L);

        // Repository mock 설정
        when(summaryRepository.findPopularSummariesByPublishStatus(PublishStatus.PUBLISHED, pageable))
                .thenReturn(summariesPage);

        // 인기도 점수 계산 결과 mock 설정
        List<Object[]> scoreResults = Arrays.asList(
                new Object[]{1L, 36.0}, // summary1: 20*0.5 + 5*0.3 + 100*0.2 = 10 + 1.5 + 20 = 31.5
                new Object[]{2L, 24.4}  // summary2: 15*0.5 + 3*0.3 + 80*0.2 = 7.5 + 0.9 + 16 = 24.4
        );
        when(summaryRepository.calculatePopularityScores(Arrays.asList(1L, 2L)))
                .thenReturn(scoreResults);

        // when
        PopularSummaryListResponse response = summaryService.getPopularSummaries(pageable);

        // then
        assertNotNull(response);
        assertEquals(2, response.summaries().size());
        assertEquals(0, response.currentPage());
        assertEquals(1, response.totalPages());
        assertEquals(2L, response.totalElements());
        assertFalse(response.hasNext());
        assertFalse(response.hasPrevious());

        // 첫 번째 요약본 검증
        PopularSummaryResponse firstSummary = response.summaries().get(0);
        assertEquals(1L, firstSummary.summaryId());
        assertEquals("인기 요약본 1", firstSummary.title());
        assertEquals("인기 요약본 1의 내용", firstSummary.brief());
        assertEquals("작성자1", firstSummary.authorName());
        assertEquals("profile1.jpg", firstSummary.authorProfileImage());
        assertEquals(100, firstSummary.viewCount());
        assertEquals(20, firstSummary.likeCount());
        assertEquals(5, firstSummary.commentCount());
        assertEquals(36.0, firstSummary.popularityScore());

        // 두 번째 요약본 검증
        PopularSummaryResponse secondSummary = response.summaries().get(1);
        assertEquals(2L, secondSummary.summaryId());
        assertEquals("인기 요약본 2", secondSummary.title());
        assertEquals(24.4, secondSummary.popularityScore());

        // Mock 호출 검증
        verify(summaryRepository, times(1)).findPopularSummariesByPublishStatus(PublishStatus.PUBLISHED, pageable);
        verify(summaryRepository, times(1)).calculatePopularityScores(Arrays.asList(1L, 2L));
    }

    @Test
    @DisplayName("인기 요약본 목록 조회 시 빈 결과 테스트")
    void getPopularSummariesEmpty() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Summary> emptySummariesPage = new PageImpl<>(Collections.emptyList(), pageable, 0L);

        when(summaryRepository.findPopularSummariesByPublishStatus(PublishStatus.PUBLISHED, pageable))
                .thenReturn(emptySummariesPage);

        // when
        PopularSummaryListResponse response = summaryService.getPopularSummaries(pageable);

        // then
        assertNotNull(response);
        assertTrue(response.summaries().isEmpty());
        assertEquals(0, response.currentPage());
        assertEquals(0, response.totalPages());
        assertEquals(0L, response.totalElements());
        assertFalse(response.hasNext());
        assertFalse(response.hasPrevious());

        // 빈 결과일 때는 인기도 점수 계산 호출되지 않음
        verify(summaryRepository, times(1)).findPopularSummariesByPublishStatus(PublishStatus.PUBLISHED, pageable);
        verify(summaryRepository, times(0)).calculatePopularityScores(any());
    }

    @Test
    @DisplayName("인기 요약본 목록 조회 시 인기도 점수가 없는 경우 테스트")
    void getPopularSummariesWithoutPopularityScore() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        Member mockMember = mock(Member.class);
        when(mockMember.getName()).thenReturn("작성자");
        when(mockMember.getProfileImage()).thenReturn("profile.jpg");

        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(1L);
        when(mockSummary.getTitle()).thenReturn("점수 없는 요약본");
        when(mockSummary.getBrief()).thenReturn("점수 없는 요약본 내용");
        when(mockSummary.getMember()).thenReturn(mockMember);
        when(mockSummary.getUpdatedAt()).thenReturn(LocalDateTime.now());
        when(mockSummary.getViewCount()).thenReturn(0);
        when(mockSummary.getLikeCount()).thenReturn(0);
        when(mockSummary.getCommentCount()).thenReturn(0);

        Page<Summary> summariesPage = new PageImpl<>(Arrays.asList(mockSummary), pageable, 1L);

        when(summaryRepository.findPopularSummariesByPublishStatus(PublishStatus.PUBLISHED, pageable))
                .thenReturn(summariesPage);

        // 인기도 점수 계산 결과가 비어있는 경우
        when(summaryRepository.calculatePopularityScores(Arrays.asList(1L)))
                .thenReturn(Collections.emptyList());

        // when
        PopularSummaryListResponse response = summaryService.getPopularSummaries(pageable);

        // then
        assertNotNull(response);
        assertEquals(1, response.summaries().size());

        PopularSummaryResponse summaryResponse = response.summaries().get(0);
        assertEquals(1L, summaryResponse.summaryId());
        assertEquals("점수 없는 요약본", summaryResponse.title());
        assertEquals(0.0, summaryResponse.popularityScore()); // 기본값 0.0

        verify(summaryRepository, times(1)).findPopularSummariesByPublishStatus(PublishStatus.PUBLISHED, pageable);
        verify(summaryRepository, times(1)).calculatePopularityScores(Arrays.asList(1L));
    }

    @Test
    @DisplayName("인기 요약본 목록 조회 시 페이징 정보 검증 테스트")
    void getPopularSummariesPagingValidation() {
        // given
        Pageable pageable = PageRequest.of(1, 5); // 두 번째 페이지, 페이지 크기 5

        // 총 12개 요약본 중 두 번째 페이지 (6~10번째) 반환
        List<Summary> summaries = createMockSummaries(5, 6); // 6번부터 10번까지 5개
        Page<Summary> summariesPage = new PageImpl<>(summaries, pageable, 12L); // 총 12개

        when(summaryRepository.findPopularSummariesByPublishStatus(PublishStatus.PUBLISHED, pageable))
                .thenReturn(summariesPage);

        // 인기도 점수 계산 결과 mock
        List<Object[]> scoreResults = Arrays.asList(
                new Object[]{6L, 30.0},
                new Object[]{7L, 25.0},
                new Object[]{8L, 20.0},
                new Object[]{9L, 15.0},
                new Object[]{10L, 10.0}
        );
        when(summaryRepository.calculatePopularityScores(Arrays.asList(6L, 7L, 8L, 9L, 10L)))
                .thenReturn(scoreResults);

        // when
        PopularSummaryListResponse response = summaryService.getPopularSummaries(pageable);

        // then
        assertNotNull(response);
        assertEquals(5, response.summaries().size());
        assertEquals(1, response.currentPage()); // 두 번째 페이지 (0-based)
        assertEquals(3, response.totalPages()); // 총 12개 / 5개 = 3페이지 (반올림)
        assertEquals(12L, response.totalElements());
        assertTrue(response.hasNext()); // 다음 페이지 있음
        assertTrue(response.hasPrevious()); // 이전 페이지 있음

        // 첫 번째 요약본이 6번인지 확인
        assertEquals(6L, response.summaries().get(0).summaryId());
        assertEquals("인기 요약본 6", response.summaries().get(0).title());

        verify(summaryRepository, times(1)).findPopularSummariesByPublishStatus(PublishStatus.PUBLISHED, pageable);
        verify(summaryRepository, times(1)).calculatePopularityScores(Arrays.asList(6L, 7L, 8L, 9L, 10L));
    }

    // Helper method for creating mock summaries
    private List<Summary> createMockSummaries(int count, int startId) {
        List<Summary> summaries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long summaryId = (long) (startId + i);

            Member mockMember = mock(Member.class);
            when(mockMember.getName()).thenReturn("작성자" + summaryId);
            when(mockMember.getProfileImage()).thenReturn("profile" + summaryId + ".jpg");

            Summary mockSummary = mock(Summary.class);
            when(mockSummary.getId()).thenReturn(summaryId);
            when(mockSummary.getTitle()).thenReturn("인기 요약본 " + summaryId);
            when(mockSummary.getBrief()).thenReturn("인기 요약본 " + summaryId + "의 내용");
            when(mockSummary.getMember()).thenReturn(mockMember);
            when(mockSummary.getUpdatedAt()).thenReturn(LocalDateTime.now());
            when(mockSummary.getViewCount()).thenReturn(10 * i);
            when(mockSummary.getLikeCount()).thenReturn(5 * i);
            when(mockSummary.getCommentCount()).thenReturn(2 * i);

            summaries.add(mockSummary);
        }
        return summaries;
    }

    private LikedSummaryResponse createMockLikedSummaryResponse(Long summaryId, String title) {
        // Create author response
        AuthorResponse author = AuthorResponse.builder()
                .id(summaryId)
                .username("작성자" + summaryId)
                .profileImageUrl("https://example.com/profiles/" + summaryId + ".png")
                .build();

        // Create tags
        List<String> tags = Arrays.asList("GPT", "ML");

        return new LikedSummaryResponse(
                summaryId,
                title,
                author,
                LocalDateTime.now(), // createdAt
                LocalDateTime.now(), // updatedAt
                5,                   // likes
                tags
        );
    }

    private List<LikedSummaryResponse> createMockLikedSummaryResponseList(int count) {
        List<LikedSummaryResponse> summaries = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            summaries.add(createMockLikedSummaryResponse((long) i, "요약본 " + i));
        }
        return summaries;
    }

}
