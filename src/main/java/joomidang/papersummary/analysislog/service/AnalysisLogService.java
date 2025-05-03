package joomidang.papersummary.analysislog.service;

import joomidang.papersummary.analysislog.entity.AnalysisLog;
import joomidang.papersummary.analysislog.entity.AnalysisStage;
import joomidang.papersummary.analysislog.exception.AnalysisLogNotFoundException;
import joomidang.papersummary.analysislog.repository.AnalysisLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AnalysisLogService {
    private final AnalysisLogRepository analysisLogRepository;

    /**
     * 논문 분석 로그를 성공으로 마킹
     */
    public void markSuccess(Long paperId, AnalysisStage stage) {
        AnalysisLog analysisLog = analysisLogRepository
                .findTopByPaperIdAndStageOrderByStartedAtDesc(paperId, stage)
                .orElseThrow(() -> new AnalysisLogNotFoundException(paperId, stage));

        analysisLog.complete();

        log.info("분석 로그 성공 처리 완료 → paperId={}, stage={}", paperId, stage);
    }

    /**
     * 논문 분석 로그를 실패로 마킹
     */
    public void markFailed(Long paperId, AnalysisStage stage, String errorMessage) {
        AnalysisLog analysisLog = analysisLogRepository
                .findTopByPaperIdAndStageOrderByStartedAtDesc(paperId, stage)
                .orElseThrow(() -> new AnalysisLogNotFoundException(paperId, stage));

        analysisLog.fail(errorMessage);

        log.info("분석 로그 실패 처리 완료 → paperId={}, stage={}, error={}", paperId, stage, errorMessage);
    }
}
