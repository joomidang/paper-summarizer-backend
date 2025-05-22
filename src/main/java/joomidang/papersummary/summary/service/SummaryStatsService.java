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
        summaryStatsRepository.increaseViewCount(summaryId);
    }

    public void increaseLikeCount(Long summaryId) {
        summaryStatsRepository.increaseLikeCount(summaryId);
    }

    public void decreaseLikeCount(Long summaryId) {
        summaryStatsRepository.decreaseLikeCount(summaryId);
    }

    public void increaseCommentCount(Long summaryId) {
        summaryStatsRepository.increaseCommentCount(summaryId);
    }

    public void decreaseCommentCount(Long summaryId) {
        summaryStatsRepository.decreaseCommentCount(summaryId);
    }
}
