package joomidang.papersummary.paper.controller;

import joomidang.papersummary.common.service.SseService;
import joomidang.papersummary.paper.service.PaperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 논문 분석 이벤트 관련 컨트롤러 SSE(Server-Sent Events)를 사용하여 클라이언트에게 실시간 이벤트를 전송
 */
@Slf4j
@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperAnalysisEventController {

    private final SseService sseService;
    private final PaperService paperService;

    /**
     * 논문 분석 이벤트 구독 엔드포인트 클라이언트는 이 엔드포인트에 연결하여 논문 분석 관련 이벤트를 수신할 수 있음
     */
    @GetMapping(value = "/{paperId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToEvents(@PathVariable Long paperId) {
        log.info("논문 분석 이벤트 구독 요청: paperId={}", paperId);
        paperService.findById(paperId);//해당 논문이 존재하는지 확인용
        return sseService.createConnection(paperId);
    }
}