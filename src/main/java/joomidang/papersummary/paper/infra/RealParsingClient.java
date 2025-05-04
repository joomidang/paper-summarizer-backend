package joomidang.papersummary.paper.infra;

import joomidang.papersummary.analysislog.entity.AnalysisStage;
import joomidang.papersummary.analysislog.service.AnalysisLogService;
import joomidang.papersummary.common.config.rabbitmq.payload.ParsingRequestedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 실제 MinerU 서버에 요청을 보내는 파싱 클라이언트
 */
@Slf4j
@Profile("!local")
@Component
@RequiredArgsConstructor
public class RealParsingClient implements ParsingClient {
    private final RestTemplate restTemplate;
    private final AnalysisLogService analysisLogService;

    @Value("${mineru.parsing-url}")
    private String parsingUrl;

    @Override
    public void requestParsing(ParsingRequestedPayload payload) {
        log.info("MinerU 파싱 서버에 요청:{}", payload.paperId());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ParsingRequestedPayload> request = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(parsingUrl, request, Void.class);
            analysisLogService.markPending(payload.paperId(), AnalysisStage.MINERU);
            log.info("MinerU 파싱 요청 전송 완료");
        } catch (Exception e) {
            log.error("MinerU 파싱 요청 실패", e);
            analysisLogService.markFailed(payload.paperId(), AnalysisStage.MINERU, e.getMessage());
            throw new RuntimeException("MinerU 파싱 요청 실패", e);
        }
    }
}
