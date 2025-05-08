package joomidang.papersummary.paper.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import joomidang.papersummary.analysislog.entity.AnalysisStage;
import joomidang.papersummary.analysislog.service.AnalysisLogService;
import joomidang.papersummary.common.config.rabbitmq.PaperEventEnvelop;
import joomidang.papersummary.common.config.rabbitmq.PaperEventPublisher;
import joomidang.papersummary.common.config.rabbitmq.PaperEventType;
import joomidang.papersummary.common.config.rabbitmq.payload.SummaryRequestedPayload;
import joomidang.papersummary.paper.controller.request.ParsingResultRequest;
import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.paper.entity.Status;
import joomidang.papersummary.paper.exception.PaperNotFoundException;
import joomidang.papersummary.paper.repository.PaperRepository;
import joomidang.papersummary.visualcontent.entity.VisualContentType;
import joomidang.papersummary.visualcontent.service.VisualContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class PaperParsingCallbackServiceTest {

    private PaperParsingCallbackService paperParsingCallbackService;
    private PaperRepository paperRepository;
    private AnalysisLogService analysisLogService;
    private VisualContentService visualContentService;
    private PaperEventPublisher paperEventPublisher;

    @BeforeEach
    void setUp() {
        paperRepository = mock(PaperRepository.class);
        analysisLogService = mock(AnalysisLogService.class);
        visualContentService = mock(VisualContentService.class);
        paperEventPublisher = mock(PaperEventPublisher.class);

        paperParsingCallbackService = new PaperParsingCallbackService(
                paperRepository,
                analysisLogService,
                visualContentService,
                paperEventPublisher
        );
    }

    @Test
    @DisplayName("파싱 콜백 처리 성공 테스트")
    void processSuccess() {
        // given
        Long paperId = 1L;
        String title = "Test Paper Title";
        String markdownUrl = "https://example.com/markdown/test-paper.md";
        String contentListUrl = "https://example.com/content-list/test-paper_content_list.json";
        List<String> figures = Arrays.asList("https://example.com/figures/fig1.png", "https://example.com/figures/fig2.png");
        List<String> tables = Arrays.asList("https://example.com/tables/table1.png");

        ParsingResultRequest request = new ParsingResultRequest(
                title,
                markdownUrl,
                contentListUrl,
                figures,
                tables
        );

        Paper mockPaper = mock(Paper.class);
        when(paperRepository.findById(paperId)).thenReturn(Optional.of(mockPaper));

        // when
        paperParsingCallbackService.process(paperId, request);

        // then
        // Verify paper updates
        verify(mockPaper, times(1)).updateStatus(Status.ANALYZED);
        verify(mockPaper, times(1)).updateTitle(title);

        // Verify analysis log updates
        verify(analysisLogService, times(1)).markSuccess(paperId, AnalysisStage.MINERU);
        verify(analysisLogService, times(1)).updateAnalysisLogStage(paperId, AnalysisStage.MINERU, AnalysisStage.GPT);
        verify(analysisLogService, times(1)).markPending(paperId, AnalysisStage.GPT);

        // Verify visual content saving
        verify(visualContentService, times(1)).saveAll(eq(mockPaper), eq(figures), eq(VisualContentType.FIGURE));
        verify(visualContentService, times(1)).saveAll(eq(mockPaper), eq(tables), eq(VisualContentType.TABLE));

        // Verify event publishing
        ArgumentCaptor<PaperEventEnvelop> eventCaptor = ArgumentCaptor.forClass(PaperEventEnvelop.class);
        verify(paperEventPublisher, times(1)).publish(eventCaptor.capture());
        
        PaperEventEnvelop<?> capturedEvent = eventCaptor.getValue();
        assert capturedEvent.type() == PaperEventType.SUMMARY_REQUESTED;
        assert capturedEvent.payload() instanceof SummaryRequestedPayload;
        
        SummaryRequestedPayload payload = (SummaryRequestedPayload) capturedEvent.payload();
        assert payload.paperId().equals(paperId);
        assert payload.markdownUrl().equals(markdownUrl);
    }

    @Test
    @DisplayName("존재하지 않는 논문 파싱 콜백 테스트")
    void processPaperNotFound() {
        // given
        Long paperId = 1L;
        ParsingResultRequest request = new ParsingResultRequest(
                "Test Title",
                "https://example.com/markdown/test.md",
                "https://example.com/content-list/test-paper_content_list.json",
                List.of(),
                List.of()
        );

        when(paperRepository.findById(paperId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(PaperNotFoundException.class, () -> {
            paperParsingCallbackService.process(paperId, request);
        });

        // Verify no interactions with other services
        verify(analysisLogService, times(0)).markSuccess(anyLong(), any(AnalysisStage.class));
        verify(visualContentService, times(0)).saveAll(any(Paper.class), anyList(), any(VisualContentType.class));
        verify(paperEventPublisher, times(0)).publish(any(PaperEventEnvelop.class));
    }

    @Test
    @DisplayName("시각 자료가 없는 경우 테스트")
    void processWithNoVisuals() {
        // given
        Long paperId = 1L;
        String title = "Test Paper Title";
        String markdownUrl = "https://example.com/markdown/test-paper.md";
        String contentListUrl = "https://example.com/content-list/test-paper_content_list.json";
        ParsingResultRequest request = new ParsingResultRequest(
                title,
                markdownUrl,
                contentListUrl,
                null,  // No figures
                null   // No tables
        );

        Paper mockPaper = mock(Paper.class);
        when(paperRepository.findById(paperId)).thenReturn(Optional.of(mockPaper));

        // when
        paperParsingCallbackService.process(paperId, request);

        // then
        // Verify paper updates
        verify(mockPaper, times(1)).updateStatus(Status.ANALYZED);
        verify(mockPaper, times(1)).updateTitle(title);

        // Verify analysis log updates
        verify(analysisLogService, times(1)).markSuccess(paperId, AnalysisStage.MINERU);

        // Verify visual content saving is not called with null lists
        verify(visualContentService, times(0)).saveAll(any(Paper.class), anyList(), any(VisualContentType.class));

        // Verify event publishing still happens
        verify(paperEventPublisher, times(1)).publish(any(PaperEventEnvelop.class));
    }
}