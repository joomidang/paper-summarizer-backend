package joomidang.papersummary.summary.consumer;

import joomidang.papersummary.common.config.rabbitmq.PaperEventEnvelop;
import joomidang.papersummary.common.config.rabbitmq.PaperEventPublisher;
import joomidang.papersummary.common.config.rabbitmq.PaperEventType;
import joomidang.papersummary.common.config.rabbitmq.RabbitMQConfig;
import joomidang.papersummary.common.config.rabbitmq.payload.SummaryCompletedPayload;
import joomidang.papersummary.common.config.rabbitmq.payload.SummaryRequestedPayload;
import joomidang.papersummary.s3.service.FakeS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 요약 요청 테스트용 컨슈머
 * <p>
 * 실제 외부 Python 서버가 없는 로컬 환경에서 사용.
 * <p>
 * SUMMARY_REQUESTED 이벤트를 수신하면 Python 서버 대신 즉시 SUMMARY_COMPLETED 이벤트를 발행하여
 * <p>
 * 다음 흐름(요약 저장 및 처리)을 테스트할 수 있게 한다.
 */
@Slf4j
@Profile("local")
@Component
@RequiredArgsConstructor
public class FakeSummaryConsumer {
    private final PaperEventPublisher paperEventPublisher;
    private final FakeS3Service fakeS3Service;

    private static final String FAKE_S3_KEY_PREFIX = "summaries/fake/";

    @RabbitListener(queues = RabbitMQConfig.SUMMARY_QUEUE)
    public void consume(PaperEventEnvelop<SummaryRequestedPayload> event) {
        SummaryRequestedPayload payload = event.payload();
        log.info("SUMMARY_REQUESTED 이벤트 수신 → paperId={}, markdownUrl={}", payload.paperId(), payload.markdownUrl());

        // 실제 Python 서버는 여기서 요약을 생성하고 S3에 저장한 후 SUMMARY_COMPLETED 이벤트를 발행함
        // 테스트를 위해 가짜 S3 키를 생성하고 가짜 마크다운 내용을 저장한 후 SUMMARY_COMPLETED 이벤트를 발행
        String fakeS3Key = generateFakeS3Key(payload.paperId());
        String fakeMarkdownContent = generateFakeMarkdownContent(payload.paperId(), payload.markdownUrl());

        // 가짜 S3 서비스에 마크다운 내용 저장
        fakeS3Service.saveMarkdownToS3(fakeS3Key, fakeMarkdownContent);
        log.info("가짜 요약 생성 완료 → paperId={}, fakeS3Key={}", payload.paperId(), fakeS3Key);

        // SUMMARY_COMPLETED 이벤트 발행
        SummaryCompletedPayload completedPayload = new SummaryCompletedPayload(payload.paperId(), fakeS3Key);
        paperEventPublisher.publish(new PaperEventEnvelop<>(
                PaperEventType.SUMMARY_COMPLETED,
                completedPayload
        ));

        log.info("SUMMARY_COMPLETED 이벤트 발행 완료 → paperId={}", payload.paperId());
    }

    private String generateFakeS3Key(Long paperId) {
        return FAKE_S3_KEY_PREFIX + paperId + "/summary-" + System.currentTimeMillis() + ".md";
    }

    /**
     * 테스트용 가짜 마크다운 내용 생성
     */
    private String generateFakeMarkdownContent(Long paperId, String originalMarkdownUrl) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# 논문 요약 (Paper ID: ").append(paperId).append(")\n\n");
        markdown.append("## 개요\n\n");
        markdown.append("이 문서는 테스트를 위한 가짜 요약 내용입니다. 실제 논문 내용은 다음 URL에서 확인할 수 있습니다: ")
                .append(originalMarkdownUrl).append("\n\n");
        markdown.append("## 주요 내용\n\n");
        markdown.append("- 첫 번째 중요 포인트\n");
        markdown.append("- 두 번째 중요 포인트\n");
        markdown.append("- 세 번째 중요 포인트\n\n");
        markdown.append("## 결론\n\n");
        markdown.append("이 논문은 중요한 연구 결과를 제시하고 있으며, 해당 분야에 큰 기여를 할 것으로 예상됩니다.\n\n");
        markdown.append("## 참고 문헌\n\n");
        markdown.append("1. 참고 문헌 1\n");
        markdown.append("2. 참고 문헌 2\n");

        return markdown.toString();
    }
}
