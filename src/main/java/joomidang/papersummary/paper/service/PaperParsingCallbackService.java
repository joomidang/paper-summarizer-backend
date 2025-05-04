package joomidang.papersummary.paper.service;

import java.util.List;
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

    public void process(Long paperId, ParsingResultRequest result) {
        // 1. 논문 조회
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new PaperNotFoundException(paperId));

        // 2. 논문 상태 업데이트
        paper.updateStatus(Status.ANALYZED);
        paper.updateTitle(result.title());

        // 3. 분석 로그 업데이트
        analysisLogService.markSuccess(paperId, AnalysisStage.MINERU);

        // 4. 시각 자료 저장
        saveVisuals(paper, result.figure(), VisualContentType.FIGURE);
        saveVisuals(paper, result.table(), VisualContentType.TABLE);

        log.info("파싱 콜백 처리 완료 → paperId={}", paperId);

        // 5. 요약 요청 이벤트 발행
        SummaryRequestedPayload payload = new SummaryRequestedPayload(
                paperId,
                result.markdownUrl(),
                "",   // 기본 프롬프트 (나중에 사용자 설정값 반영 가능)
                "ko"
        );

        analysisLogService.updateAnalysisLogStage(paperId, AnalysisStage.MINERU, AnalysisStage.GPT);
        analysisLogService.markPending(paperId, AnalysisStage.GPT);

        paperEventPublisher.publish(new PaperEventEnvelop<>(
                PaperEventType.SUMMARY_REQUESTED,
                payload
        ));
    }

    private void saveVisuals(Paper paper, List<String> urls, VisualContentType type) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        visualContentService.saveAll(paper, urls, type);
    }
}
