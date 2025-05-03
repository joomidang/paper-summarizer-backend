package joomidang.papersummary.paper.infra;

import joomidang.papersummary.common.config.rabbitmq.PaperEventPublisher;
import joomidang.papersummary.common.config.rabbitmq.payload.ParsingRequestedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 파싱 요청 테스트용 클라이언트
 * <p>
 * 실제 외부 파싱 서버가 없는 로컬 환경에서 사용.
 * <p>
 * 파싱 요청이 들어오면 MinerU 대신 즉시 SUMMARY_REQUESTED 이벤트를 발행하여
 * <p>
 * 다음 흐름(요약 생성)을 테스트할 수 있게 한다.
 */

@Slf4j
@Profile("local")
@Component
@RequiredArgsConstructor
public class FakeParsingClient implements ParsingClient {
    private final PaperEventPublisher paperEventPublisher;

    @Override
    public void requestParsing(ParsingRequestedPayload payload) {
        log.info("minerU 파싱 요청 시뮬레이션: paperId={}, url={}", payload.paperId(), payload.s3Url());
        //실제 RealParsingClient에서는 이렇게 사용하면 안됨 그냥 minerU client를 만들어서 호출을 보내야함
        //paperEventPublisher.publish(new PaperEventEnvelop<>(PaperEventType.SUMMARY_REQUESTED, payload));
    }
}