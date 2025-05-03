package joomidang.papersummary.analysislog.exception;

import joomidang.papersummary.analysislog.entity.AnalysisStage;
import lombok.Getter;

@Getter
public class AnalysisLogNotFoundException extends RuntimeException {
    public AnalysisLogNotFoundException(Long paperId, AnalysisStage stage) {
        super("해당 논문의 분석 로그를 찾을 수 없습니다. paperId=" + paperId + ", stage=" + stage.name());
    }

}
