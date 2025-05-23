package joomidang.papersummary.summary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import joomidang.papersummary.common.config.rabbitmq.StatsEventPublisher;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.service.MemberService;
import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.paper.exception.AccessDeniedException;
import joomidang.papersummary.paper.service.PaperService;
import joomidang.papersummary.s3.service.S3Service;
import joomidang.papersummary.summary.controller.request.SummaryEditRequest;
import joomidang.papersummary.summary.controller.response.SummaryDetailResponse;
import joomidang.papersummary.summary.controller.response.SummaryEditDetailResponse;
import joomidang.papersummary.summary.controller.response.SummaryEditResponse;
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

public class SummaryServiceTest {

    private SummaryService summaryService;
    private PaperService paperService;
    private SummaryRepository summaryRepository;
    private SummaryStatsRepository summaryStatsRepository;
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
        visualContentService = mock(VisualContentService.class);
        memberService = mock(MemberService.class);
        s3Service = mock(S3Service.class);
        summaryVersionService = mock(SummaryVersionService.class);
        statsEventPublisher = mock(StatsEventPublisher.class);

        summaryService = new SummaryService(
                paperService,
                summaryRepository,
                summaryStatsRepository,
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
}
