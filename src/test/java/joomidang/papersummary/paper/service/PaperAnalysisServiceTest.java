package joomidang.papersummary.paper.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import joomidang.papersummary.common.config.rabbitmq.PaperEventEnvelop;
import joomidang.papersummary.common.config.rabbitmq.PaperEventPublisher;
import joomidang.papersummary.common.config.rabbitmq.PaperEventType;
import joomidang.papersummary.common.config.rabbitmq.payload.ParsingRequestedPayload;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.service.MemberService;
import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.paper.exception.AccessDeniedException;
import joomidang.papersummary.paper.exception.PaperNotFoundException;
import joomidang.papersummary.paper.repository.PaperRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class PaperAnalysisServiceTest {

    private PaperAnalysisService paperAnalysisService;
    private MemberService memberService;
    private PaperRepository paperRepository;
    private PaperEventPublisher paperEventPublisher;

    @BeforeEach
    void setUp() {
        memberService = mock(MemberService.class);
        paperRepository = mock(PaperRepository.class);
        paperEventPublisher = mock(PaperEventPublisher.class);

        paperAnalysisService = new PaperAnalysisService(
                memberService,
                paperRepository,
                paperEventPublisher
        );
    }

    @Test
    @DisplayName("논문 분석 요청 성공 테스트")
    void requestParsingSuccess() {
        // given
        Long paperId = 1L;
        String providerUid = "test-provider-uid";
        String prompt = "test prompt";
        String language = "ko";

        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        Paper mockPaper = mock(Paper.class);
        when(mockPaper.getId()).thenReturn(paperId);
        when(mockPaper.getFilePath()).thenReturn("https://example.com/papers/test-paper.pdf");
        when(mockPaper.hasNotPermission(mockMember.getId())).thenReturn(false);
        when(paperRepository.findById(paperId)).thenReturn(Optional.of(mockPaper));

        // when
        paperAnalysisService.requestParsing(paperId, providerUid, prompt, language);

        // then
        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(paperRepository, times(1)).findById(paperId);

        // Verify event publishing
        ArgumentCaptor<PaperEventEnvelop> eventCaptor = ArgumentCaptor.forClass(PaperEventEnvelop.class);
        verify(paperEventPublisher, times(1)).publish(eventCaptor.capture());

        PaperEventEnvelop<?> capturedEvent = eventCaptor.getValue();
        assert capturedEvent.type() == PaperEventType.PARSING_REQUESTED;
        assert capturedEvent.payload() instanceof ParsingRequestedPayload;

        ParsingRequestedPayload payload = (ParsingRequestedPayload) capturedEvent.payload();
        assert payload.paperId().equals(paperId);
        assert payload.userId().equals(mockMember.getId());
        assert payload.s3Url().equals(mockPaper.getFilePath());
    }

    @Test
    @DisplayName("존재하지 않는 논문 분석 요청 테스트")
    void requestParsingPaperNotFound() {
        // given
        Long paperId = 1L;
        String providerUid = "test-provider-uid";
        String prompt = "test prompt";
        String language = "ko";

        when(paperRepository.findById(paperId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(PaperNotFoundException.class, () -> {
            paperAnalysisService.requestParsing(paperId, providerUid, prompt, language);
        });

        verify(paperRepository, times(1)).findById(paperId);
        verify(memberService, never()).findByProviderUid(any());
        verify(paperEventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("권한 없는 사용자의 논문 분석 요청 테스트")
    void requestParsingAccessDenied() {
        // given
        Long paperId = 1L;
        String providerUid = "test-provider-uid";
        String prompt = "test prompt";
        String language = "ko";

        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        Paper mockPaper = mock(Paper.class);
        when(mockPaper.getId()).thenReturn(paperId);
        when(mockPaper.hasNotPermission(mockMember.getId())).thenReturn(true);
        when(paperRepository.findById(paperId)).thenReturn(Optional.of(mockPaper));

        // when & then
        assertThrows(AccessDeniedException.class, () -> {
            paperAnalysisService.requestParsing(paperId, providerUid, prompt, language);
        });

        verify(paperRepository, times(1)).findById(paperId);
        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(paperEventPublisher, never()).publish(any());
    }
}
