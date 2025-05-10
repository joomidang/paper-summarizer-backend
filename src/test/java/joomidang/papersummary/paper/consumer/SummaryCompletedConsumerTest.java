package joomidang.papersummary.paper.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import joomidang.papersummary.analysislog.entity.AnalysisStage;
import joomidang.papersummary.analysislog.service.AnalysisLogService;
import joomidang.papersummary.common.config.rabbitmq.PaperEventEnvelop;
import joomidang.papersummary.common.config.rabbitmq.payload.SummaryCompletedPayload;
import joomidang.papersummary.common.service.SseService;
import joomidang.papersummary.summary.exception.AlreadySummarizedException;
import joomidang.papersummary.summary.service.SummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SummaryCompletedConsumerTest {

    private SummaryCompletedConsumer summaryCompletedConsumer;
    private SummaryService summaryService;
    private AnalysisLogService analysisLogService;
    private SseService sseService;

    @BeforeEach
    void setUp() {
        summaryService = mock(SummaryService.class);
        analysisLogService = mock(AnalysisLogService.class);
        sseService = mock(SseService.class);

        summaryCompletedConsumer = new SummaryCompletedConsumer(
                summaryService,
                analysisLogService,
                sseService
        );
    }

    @Test
    @DisplayName("요약 완료 이벤트 처리 성공 테스트")
    void consumeSuccess() {
        // given
        Long paperId = 1L;
        String s3Key = "test-s3-key.md";
        Long summaryId = 10L;

        SummaryCompletedPayload payload = new SummaryCompletedPayload(paperId, s3Key);
        PaperEventEnvelop<SummaryCompletedPayload> event = new PaperEventEnvelop<>(null, payload);

        when(summaryService.createSummaryFromS3(paperId, s3Key)).thenReturn(summaryId);
        when(sseService.sendSummaryCompletedEvent(paperId, summaryId)).thenReturn(true);

        // when
        summaryCompletedConsumer.consume(event);

        // then
        verify(analysisLogService, times(1)).markSuccess(paperId, AnalysisStage.GPT);
        verify(summaryService, times(1)).createSummaryFromS3(paperId, s3Key);
        verify(sseService, times(1)).sendSummaryCompletedEvent(paperId, summaryId);
    }

    @Test
    @DisplayName("요약 완료 이벤트 처리 성공 - SSE 전송 실패 테스트")
    void consumeSuccessButSseFailed() {
        // given
        Long paperId = 1L;
        String s3Key = "test-s3-key.md";
        Long summaryId = 10L;

        SummaryCompletedPayload payload = new SummaryCompletedPayload(paperId, s3Key);
        PaperEventEnvelop<SummaryCompletedPayload> event = new PaperEventEnvelop<>(null, payload);

        when(summaryService.createSummaryFromS3(paperId, s3Key)).thenReturn(summaryId);
        when(sseService.sendSummaryCompletedEvent(paperId, summaryId)).thenReturn(false);

        // when
        summaryCompletedConsumer.consume(event);

        // then
        verify(analysisLogService, times(1)).markSuccess(paperId, AnalysisStage.GPT);
        verify(summaryService, times(1)).createSummaryFromS3(paperId, s3Key);
        verify(sseService, times(1)).sendSummaryCompletedEvent(paperId, summaryId);
    }

    @Test
    @DisplayName("이미 요약된 논문 처리 테스트")
    void consumeAlreadySummarized() {
        // given
        Long paperId = 1L;
        String s3Key = "test-s3-key.md";

        SummaryCompletedPayload payload = new SummaryCompletedPayload(paperId, s3Key);
        PaperEventEnvelop<SummaryCompletedPayload> event = new PaperEventEnvelop<>(null, payload);

        when(summaryService.createSummaryFromS3(paperId, s3Key))
                .thenThrow(new AlreadySummarizedException("이미 요약된 논문입니다."));

        // when
        summaryCompletedConsumer.consume(event);

        // then
        verify(analysisLogService, times(1)).markSuccess(paperId, AnalysisStage.GPT);
        verify(summaryService, times(1)).createSummaryFromS3(paperId, s3Key);
        verify(sseService, times(0)).sendSummaryCompletedEvent(anyLong(), anyLong());
    }

    @Test
    @DisplayName("예외 발생 테스트")
    void consumeException() {
        // given
        Long paperId = 1L;
        String s3Key = "test-s3-key.md";

        SummaryCompletedPayload payload = new SummaryCompletedPayload(paperId, s3Key);
        PaperEventEnvelop<SummaryCompletedPayload> event = new PaperEventEnvelop<>(null, payload);

        when(summaryService.createSummaryFromS3(paperId, s3Key))
                .thenThrow(new RuntimeException("테스트 예외"));

        // when
        summaryCompletedConsumer.consume(event);

        // then
        verify(analysisLogService, times(1)).markSuccess(paperId, AnalysisStage.GPT);
        verify(summaryService, times(1)).createSummaryFromS3(paperId, s3Key);
        verify(sseService, times(0)).sendSummaryCompletedEvent(anyLong(), anyLong());
    }
}