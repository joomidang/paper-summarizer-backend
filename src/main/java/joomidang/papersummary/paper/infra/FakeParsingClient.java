package joomidang.papersummary.paper.infra;

import joomidang.papersummary.common.config.rabbitmq.PaperEvent;
import joomidang.papersummary.common.config.rabbitmq.PaperEventPublisher;
import joomidang.papersummary.common.config.rabbitmq.PaperEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("local")
@Component
@RequiredArgsConstructor
public class FakeParsingClient implements ParsingClient {
    private final PaperEventPublisher paperEventPublisher;

    @Override
    public void requestParsing(Long paperId, Long userId, String url) {
        log.info("minerU 파싱 요청 시뮬레이션: paperId={}, url={}", paperId, url);

        paperEventPublisher.publish(new PaperEvent(PaperEventType.PARSING_REQUESTED, paperId));
    }
}