package joomidang.papersummary.paper.consumer;

import joomidang.papersummary.analysislog.entity.AnalysisStage;
import joomidang.papersummary.analysislog.service.AnalysisLogService;
import joomidang.papersummary.common.config.rabbitmq.PaperEventEnvelop;
import joomidang.papersummary.common.config.rabbitmq.RabbitMQConfig;
import joomidang.papersummary.common.config.rabbitmq.payload.SummaryCompletedPayload;
import joomidang.papersummary.summary.exception.AlreadySummarizedException;
import joomidang.papersummary.summary.service.SummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class SummaryCompletedConsumer {
    private final SummaryService summaryService;
    private final AnalysisLogService analysisLogService;

    @RabbitListener(queues = RabbitMQConfig.COMPLETE_QUEUE)
    public void consume(PaperEventEnvelop<SummaryCompletedPayload> event) {
        try {
            SummaryCompletedPayload payload = event.payload();
            log.info("SUMMARY_COMPLETED 수신 → paperId={}, s3Key={}", payload.paperId(), payload.s3Key());
            analysisLogService.markSuccess(payload.paperId(), AnalysisStage.GPT);
            //Summary 저장
            summaryService.createSummaryFromS3(payload.paperId(), payload.s3Key());
        } catch (AlreadySummarizedException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error("SUMMARY_COMPLETE 처리 실패", e);
            // Dead-letter queue 또는 재시도 정책 고려
        }
    }
}
