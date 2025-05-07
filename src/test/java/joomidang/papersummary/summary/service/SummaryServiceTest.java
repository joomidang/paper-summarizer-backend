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
import java.util.Collections;
import java.util.Optional;
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
import joomidang.papersummary.summary.entity.SummaryVersion;
import joomidang.papersummary.summary.exception.SummaryCreationFailedException;
import joomidang.papersummary.summary.exception.SummaryNotFoundException;
import joomidang.papersummary.summary.repository.SummaryRepository;
import joomidang.papersummary.visualcontent.service.VisualContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class SummaryServiceTest {

    private SummaryService summaryService;
    private PaperService paperService;
    private SummaryRepository summaryRepository;
    private VisualContentService visualContentService;
    private MemberService memberService;
    private S3Service s3Service;
    private SummaryVersionService summaryVersionService;

    @BeforeEach
    void setUp() {
        paperService = mock(PaperService.class);
        summaryRepository = mock(SummaryRepository.class);
        visualContentService = mock(VisualContentService.class);
        memberService = mock(MemberService.class);
        s3Service = mock(S3Service.class);
        summaryVersionService = mock(SummaryVersionService.class);

        summaryService = new SummaryService(
                paperService,
                summaryRepository,
                visualContentService,
                memberService,
                s3Service,
                summaryVersionService
        );
    }

    @Test
    @DisplayName("S3에서 요약 생성 성공 테스트")
    void createSummaryFromS3Success() {
        // given
        Long paperId = 1L;
        String s3Key = "test-s3-key.md";

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
                .viewCount(0)
                .likeCount(0)
                .paper(mockPaper)
                .member(mockMember)
                .build();

        when(summaryRepository.save(any(Summary.class))).thenReturn(mockSummary);

        // when
        summaryService.createSummaryFromS3(paperId, s3Key);

        // then
        verify(paperService, times(1)).findById(paperId);
        verify(summaryRepository, times(1)).existsByPaper(mockPaper);
        verify(visualContentService, times(1)).connectToSummary(any(Summary.class));
        verify(summaryRepository, times(1)).save(any(Summary.class));

        // Verify Summary entity creation
        ArgumentCaptor<Summary> summaryCaptor = ArgumentCaptor.forClass(Summary.class);
        verify(summaryRepository).save(summaryCaptor.capture());
        Summary capturedSummary = summaryCaptor.getValue();
        assertEquals("Test Paper Title", capturedSummary.getTitle());
        assertEquals(s3Key, capturedSummary.getS3KeyMd());
        assertEquals(PublishStatus.DRAFT, capturedSummary.getPublishStatus());
        assertEquals(0, capturedSummary.getViewCount());
        assertEquals(0, capturedSummary.getLikeCount());
        assertEquals(mockPaper, capturedSummary.getPaper());
        assertEquals(mockMember, capturedSummary.getMember());
    }

    @Test
    @DisplayName("S3 키가 유효하지 않을 때 예외 발생 테스트")
    void createSummaryFromS3InvalidS3Key() {
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
    void createSummaryFromS3ExistingSummary() {
        // given
        Long paperId = 1L;
        String s3Key = "test-s3-key.md";

        Paper mockPaper = mock(Paper.class);
        when(mockPaper.getId()).thenReturn(paperId);
        when(paperService.findById(paperId)).thenReturn(mockPaper);

        when(summaryRepository.existsByPaper(mockPaper)).thenReturn(true);

        // when & then
        assertThrows(SummaryCreationFailedException.class, () -> {
            summaryService.createSummaryFromS3(paperId, s3Key);
        });

        // Verify interactions
        verify(paperService, times(1)).findById(paperId);
        verify(summaryRepository, times(1)).existsByPaper(mockPaper);
        verify(visualContentService, times(0)).connectToSummary(any(Summary.class));
        verify(summaryRepository, times(0)).save(any(Summary.class));
    }

    @Test
    @DisplayName("ID로 요약 조회 성공 테스트")
    void findByIdSuccess() {
        // given
        Long summaryId = 1L;
        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(summaryId);
        when(summaryRepository.findById(summaryId)).thenReturn(Optional.of(mockSummary));

        // when
        Summary result = summaryService.findById(summaryId);

        // then
        assertNotNull(result);
        assertEquals(summaryId, result.getId());
        verify(summaryRepository, times(1)).findById(summaryId);
    }

    @Test
    @DisplayName("존재하지 않는 요약 ID로 조회 시 예외 발생 테스트")
    void findByIdNotFound() {
        // given
        Long summaryId = 1L;
        when(summaryRepository.findById(summaryId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(SummaryNotFoundException.class, () -> {
            summaryService.findById(summaryId);
        });

        verify(summaryRepository, times(1)).findById(summaryId);
    }

    @Test
    @DisplayName("요약본 편집을 위한 상세 정보 조회 성공 테스트")
    void getSummaryForEditSuccess() {
        // given
        String providerUid = "test-provider-uid";
        Long summaryId = 1L;

        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(summaryId);
        when(mockSummary.getTitle()).thenReturn("Test Summary Title");
        when(mockSummary.getBrief()).thenReturn("Test Brief");
        when(mockSummary.getS3KeyMd()).thenReturn("original-s3-key.md");
        when(mockSummary.isNotSameMemberId(1L)).thenReturn(false);
        when(summaryRepository.findById(summaryId)).thenReturn(Optional.of(mockSummary));

        // No draft version found, use original s3Key
        when(summaryVersionService.findLatestDraft(summaryId)).thenReturn(Optional.empty());

        String markdownUrl = "https://paper-dev-test-magic-pdf-output.s3.bucket.com/original-s3-key.md";

        // when
        SummaryEditDetailResponse response = summaryService.getSummaryForEdit(providerUid, summaryId);

        // then
        assertNotNull(response);
        assertEquals(mockSummary.getTitle(), response.getTitle());
        assertEquals(mockSummary.getBrief(), response.getBrief());
        assertEquals(markdownUrl, response.getMarkdownUrl());

        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(summaryRepository, times(1)).findById(summaryId);
        verify(summaryVersionService, times(1)).findLatestDraft(summaryId);
    }

    @Test
    @DisplayName("요약본 편집을 위한 상세 정보 조회 시 최신 드래프트 버전 사용 테스트")
    void getSummaryForEditWithLatestDraft() {
        // given
        String providerUid = "test-provider-uid";
        Long summaryId = 1L;

        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(summaryId);
        when(mockSummary.getTitle()).thenReturn("Test Summary Title");
        when(mockSummary.getBrief()).thenReturn("Test Brief");
        when(mockSummary.getS3KeyMd()).thenReturn("original-s3-key.md");
        when(mockSummary.isNotSameMemberId(1L)).thenReturn(false);
        when(summaryRepository.findById(summaryId)).thenReturn(Optional.of(mockSummary));

        // Draft version found
        SummaryVersion mockVersion = mock(SummaryVersion.class);
        when(mockVersion.getS3KeyMd()).thenReturn("draft-s3-key.md");
        when(summaryVersionService.findLatestDraft(summaryId)).thenReturn(Optional.of(mockVersion));

        String markdownUrl = "https://paper-dev-test-magic-pdf-output.s3.bucket.com/draft-s3-key.md";

        // when
        SummaryEditDetailResponse response = summaryService.getSummaryForEdit(providerUid, summaryId);

        // then
        assertNotNull(response);
        assertEquals(mockSummary.getTitle(), response.getTitle());
        assertEquals(mockSummary.getBrief(), response.getBrief());
        assertEquals(markdownUrl, response.getMarkdownUrl());

        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(summaryRepository, times(1)).findById(summaryId);
        verify(summaryVersionService, times(1)).findLatestDraft(summaryId);
    }

    @Test
    @DisplayName("권한 없는 사용자의 요약본 편집 정보 조회 시 예외 발생 테스트")
    void getSummaryForEditAccessDenied() {
        // given
        String providerUid = "test-provider-uid";
        Long summaryId = 1L;

        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(summaryId);
        when(mockSummary.isNotSameMemberId(1L)).thenReturn(true); // Different member
        when(summaryRepository.findById(summaryId)).thenReturn(Optional.of(mockSummary));

        // when & then
        assertThrows(AccessDeniedException.class, () -> {
            summaryService.getSummaryForEdit(providerUid, summaryId);
        });

        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(summaryRepository, times(1)).findById(summaryId);
        verify(summaryVersionService, times(0)).findLatestDraft(anyLong());
    }

    @Test
    @DisplayName("요약본 편집 내용 저장 성공 테스트")
    void saveSummaryEditSuccess() {
        // given
        String providerUid = "test-provider-uid";
        Long summaryId = 1L;
        SummaryEditRequest request = new SummaryEditRequest(
                "Updated Title",
                "Updated Brief",
                "Updated markdown content",
                Collections.emptyList()
        );

        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(summaryId);
        when(mockSummary.getPublishStatus()).thenReturn(PublishStatus.DRAFT);
        when(mockSummary.isNotSameMemberId(1L)).thenReturn(false);
        when(summaryRepository.findById(summaryId)).thenReturn(Optional.of(mockSummary));

        String s3Key = "summaries/" + summaryId + "/draft-" + System.currentTimeMillis() + ".md";
        String markdownUrl = "https://paper-dev-test-magic-pdf-output.s3.bucket.com/" + s3Key;
        when(s3Service.saveMarkdownToS3(anyString(), eq(request.markdownContent()))).thenReturn(markdownUrl);

        // when
        SummaryEditResponse response = summaryService.saveSummaryEdit(providerUid, summaryId, request);

        // then
        assertNotNull(response);
        assertEquals(summaryId, response.getSummaryId());
        assertEquals(PublishStatus.DRAFT, response.getStatus());
        assertEquals(markdownUrl, response.getMarkdownUrl());
        assertNotNull(response.getSavedAt());

        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(summaryRepository, times(1)).findById(summaryId);
        verify(s3Service, times(1)).saveMarkdownToS3(anyString(), eq(request.markdownContent()));
        verify(summaryVersionService, times(1)).createDraftVersion(eq(mockSummary), anyString(), eq(request.title()),
                eq(mockMember));
    }

    @Test
    @DisplayName("요약본 발행 성공 테스트")
    void publishSummarySuccess() {
        // given
        String providerUid = "test-provider-uid";
        Long summaryId = 1L;
        SummaryEditRequest request = new SummaryEditRequest(
                "Published Title",
                "Published Brief",
                "Published markdown content",
                Collections.emptyList()
        );

        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(summaryId);
        when(mockSummary.getTitle()).thenReturn("Published Title");
        when(mockSummary.getUpdatedAt()).thenReturn(LocalDateTime.now());
        when(mockSummary.isNotSameMemberId(1L)).thenReturn(false);
        when(summaryRepository.findById(summaryId)).thenReturn(Optional.of(mockSummary));
        when(summaryRepository.save(mockSummary)).thenReturn(mockSummary);

        String s3Key = "summaries/" + summaryId + "/publish-" + System.currentTimeMillis() + ".md";
        String markdownUrl = "https://paper-dev-test-magic-pdf-output.s3.bucket.com/" + s3Key;
        when(s3Service.saveMarkdownToS3(anyString(), eq(request.markdownContent()))).thenReturn(markdownUrl);

        // when
        SummaryPublishResponse response = summaryService.publishSummary(providerUid, summaryId, request);

        // then
        assertNotNull(response);
        assertEquals(summaryId, response.summaryId());
        assertEquals(markdownUrl, response.markdownUrl());
        assertEquals("Published Title", response.title());
        assertNotNull(response.publishedAt());

        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(summaryRepository, times(1)).findById(summaryId);
        verify(s3Service, times(1)).saveMarkdownToS3(anyString(), eq(request.markdownContent()));
        verify(summaryVersionService, times(1)).createPublishedVersion(eq(mockSummary), anyString(),
                eq(request.title()), eq(mockMember));
        verify(mockSummary, times(1)).publish(eq(request.title()), eq(request.brief()), anyString());
        verify(summaryRepository, times(1)).save(mockSummary);
    }

    @Test
    @DisplayName("발행된 요약본 상세 조회 성공 테스트")
    void getSummaryDetailSuccess() {
        // given
        Long summaryId = 1L;
        String s3Key = "test-s3-key.md";
        String markdownUrl = "https://paper-dev-test-magic-pdf-output.s3.bucket.com/" + s3Key;
        LocalDateTime updatedAt = LocalDateTime.now();

        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(summaryId);
        when(mockSummary.getTitle()).thenReturn("Test Summary Title");
        when(mockSummary.getBrief()).thenReturn("Test Brief");
        when(mockSummary.getS3KeyMd()).thenReturn(s3Key);
        when(mockSummary.getPublishStatus()).thenReturn(PublishStatus.PUBLISHED);
        when(mockSummary.getUpdatedAt()).thenReturn(updatedAt);
        when(mockSummary.getViewCount()).thenReturn(10);
        when(mockSummary.getLikeCount()).thenReturn(5);

        when(summaryRepository.findById(summaryId)).thenReturn(Optional.of(mockSummary));

        // when
        SummaryDetailResponse response = summaryService.getSummaryDetail(summaryId);

        // then
        assertNotNull(response);
        assertEquals(summaryId, response.summaryId());
        assertEquals("Test Summary Title", response.title());
        assertEquals("Test Brief", response.brief());
        assertEquals(markdownUrl, response.markdownUrl());
        assertEquals(Collections.emptyList(), response.tags());
        assertEquals(updatedAt, response.publishedAt());
        assertEquals(10, response.viewCount());
        assertEquals(5, response.likeCount());

        verify(summaryRepository, times(1)).findById(summaryId);
    }

    @Test
    @DisplayName("발행되지 않은 요약본 상세 조회 시 예외 발생 테스트")
    void getSummaryDetailAccessDenied() {
        // given
        Long summaryId = 1L;

        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(summaryId);
        when(mockSummary.getPublishStatus()).thenReturn(PublishStatus.DRAFT); // Not published

        when(summaryRepository.findById(summaryId)).thenReturn(Optional.of(mockSummary));

        // when & then
        assertThrows(AccessDeniedException.class, () -> {
            summaryService.getSummaryDetail(summaryId);
        });

        verify(summaryRepository, times(1)).findById(summaryId);
    }
}
