package joomidang.papersummary.paper.consumer;

import joomidang.papersummary.common.config.rabbitmq.PaperEventEnvelop;
import joomidang.papersummary.common.config.rabbitmq.RabbitMQConfig;
import joomidang.papersummary.common.config.rabbitmq.payload.ParsingRequestedPayload;
import joomidang.papersummary.paper.infra.ParsingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 논문 파싱 요청 Consumer
 * <p>
 * PARSING_REQUESTED 이벤트를 수신하면:
 * <p>
 * - 논문 ID, 사용자 ID, S3 경로 정보를 통해
 * <p>
 * - 외부 파싱 서버(MinerU)로 파싱 요청을 전송한다
 * <p>
 * 실제 파싱 로직은 ParsingClient(Fake or Real)에 위임되며, 이 클래스는 메시지 수신자 역할에 집중한다.
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class PaperParsingConsumer {
    private final ParsingClient parsingClient;

    @RabbitListener(queues = RabbitMQConfig.PARSING_QUEUE)
    public void consume(PaperEventEnvelop<ParsingRequestedPayload> event) {
        ParsingRequestedPayload payload = event.payload();
        log.info("파싱 요청 이벤트 수신: paperId={}, userId={}, s3Url={}", 
                payload.paperId(), payload.userId(), payload.s3Url());

        try {
            log.debug("외부 파싱 서버로 요청 전송 시작: paperId={}", payload.paperId());
            parsingClient.requestParsing(payload);
            log.info("파싱 요청 처리 완료: paperId={}", payload.paperId());
        } catch (Exception e) {
            log.error("파싱 요청 처리 중 오류 발생: paperId={}, 오류={}", 
                    payload.paperId(), e.getMessage(), e);
            throw e;
        }
    }
}
