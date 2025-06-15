package joomidang.papersummary.paper.service;

import java.util.List;
import joomidang.papersummary.analysislog.entity.AnalysisStage;
import joomidang.papersummary.analysislog.service.AnalysisLogService;
import joomidang.papersummary.common.config.rabbitmq.PaperEventEnvelop;
import joomidang.papersummary.common.config.rabbitmq.PaperEventPublisher;
import joomidang.papersummary.common.config.rabbitmq.PaperEventType;
import joomidang.papersummary.common.config.rabbitmq.payload.SummaryRequestedPayload;
import joomidang.papersummary.common.service.SseService;
import joomidang.papersummary.paper.controller.request.ParsingResultRequest;
import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.paper.entity.Status;
import joomidang.papersummary.paper.exception.PaperNotFoundException;
import joomidang.papersummary.paper.repository.PaperRepository;
import joomidang.papersummary.visualcontent.entity.VisualContentType;
import joomidang.papersummary.visualcontent.service.VisualContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MinerU 파싱 결과 콜백 처리 서비스
 * <p>
 * 외부 파싱 서버에서 논문 파싱을 완료하고 콜백을 보내면:
 * <p>
 * - 논문 상태를 ANALYZED로 변경하고
 * <p>
 * - analysis_log를 성공으로 업데이트하며
 * <p>
 * - 시각 자료, 마크다운 URL 등을 저장한 후
 * <p>
 * - 요약 요청(SUMMARY_REQUESTED) 이벤트를 발행한다
 */

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PaperParsingCallbackService {

    private final PaperRepository paperRepository;
    private final AnalysisLogService analysisLogService;
    private final VisualContentService visualContentService;
    private final PaperEventPublisher paperEventPublisher;
    private final SseService sseService;

    public void process(Long paperId, ParsingResultRequest result) {
        // 1. 논문 조회
        Paper paper = findPaperById(paperId);

        // 2. 논문 상태 업데이트
        updatePaperDetails(result, paper);

        // 3. 분석 로그 업데이트
        updateAnalysisLog(paperId);

        // 4. 시각 자료 저장
        saveVisualContents(result, paper);

        log.info("파싱 콜백 처리 완료 → paperId={}", paperId);

        // 5. SSE를 통해 클라이언트에게 파싱 완료 이벤트 전송
        notifyClientViaSSE(paperId);

        // 6. 요약 요청 이벤트 발행
        requestSummaryGeneration(paperId, result);
    }

    private Paper findPaperById(Long paperId) {
        return paperRepository.findById(paperId)
                .orElseThrow(() -> new PaperNotFoundException(paperId));
    }

    private void updatePaperDetails(ParsingResultRequest result, Paper paper) {
        paper.updateStatus(Status.ANALYZED);
        paper.updateTitle(result.title());
    }

    private void updateAnalysisLog(Long paperId) {
        analysisLogService.markSuccess(paperId, AnalysisStage.MINERU);
    }

    private void saveVisualContents(ParsingResultRequest result, Paper paper) {
        saveVisuals(paper, result.figures(), VisualContentType.FIGURE);
        saveVisuals(paper, result.tables(), VisualContentType.TABLE);
    }

    private void saveVisuals(Paper paper, List<String> urls, VisualContentType type) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        visualContentService.saveAll(paper, urls, type);
    }

    private void notifyClientViaSSE(Long paperId) {
        boolean sent = sseService.sendParsingCompletedEvent(paperId);
        if (sent) {
            log.info("파싱 완료 이벤트 전송 성공: paperId={}", paperId);
        } else {
            log.warn("파싱 완료 이벤트 전송 실패 (연결된 클라이언트 없음): paperId={}", paperId);
        }
    }

    private void requestSummaryGeneration(Long paperId, ParsingResultRequest result) {
        SummaryRequestedPayload payload = getSummaryRequestedPayload(
                paperId, result);

        //분석 로그 상태 업데이트
        prepareAnalysisLogForSummary(paperId);

        //이벤트 발행
        publishSummaryRequestedEvent(payload);
    }

    private SummaryRequestedPayload getSummaryRequestedPayload(Long paperId, ParsingResultRequest result) {
        //TODO 기본 프롬프트 (나중에 사용자 설정값 반영 가능)
        return new SummaryRequestedPayload(
                paperId,
                result.markdownUrl(),
                result.contentListUrl(),
                "",   // 기본 프롬프트 (나중에 사용자 설정값 반영 가능)
                "ko"
        );
    }

    private void prepareAnalysisLogForSummary(Long paperId) {
        analysisLogService.updateAnalysisLogStage(paperId, AnalysisStage.MINERU, AnalysisStage.GPT);
        analysisLogService.markPending(paperId, AnalysisStage.GPT);
    }

    private void publishSummaryRequestedEvent(SummaryRequestedPayload payload) {
        paperEventPublisher.publish(new PaperEventEnvelop<>(
                PaperEventType.SUMMARY_REQUESTED,
                payload
        ));
    }
}
