package joomidang.papersummary.summary.service;

import joomidang.papersummary.summary.repository.SummaryRepository;
import joomidang.papersummary.summary.repository.SummaryStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SummaryStatsService {
    private final SummaryStatsRepository summaryStatsRepository;
    private final SummaryRepository summaryRepository;

    public void increaseViewCount(Long summaryId) {
        int result = summaryStatsRepository.increaseViewCount(summaryId);
        log.debug("조회수 증가 summaryId={}, result={}", summaryId, result);
    }

    public void increaseLikeCount(Long summaryId) {
        int result = summaryStatsRepository.increaseLikeCount(summaryId);
        log.debug("좋아요 수 증가 summaryId={}, result={}", summaryId, result);
    }

    public void decreaseLikeCount(Long summaryId) {
        int result = summaryStatsRepository.decreaseLikeCount(summaryId);
        log.debug("좋아요 수 감소 summaryId={}, result={}", summaryId, result);
    }

    public void increaseCommentCount(Long summaryId) {
        int result = summaryStatsRepository.increaseCommentCount(summaryId);
        log.debug("댓글 수 증가 summaryId={}, result={}", summaryId, result);
    }

    public void decreaseCommentCount(Long summaryId) {
        int result = summaryStatsRepository.decreaseCommentCount(summaryId);
        log.debug("댓글 수 감소 summaryId={}, result={}", summaryId, result);
    }
}
