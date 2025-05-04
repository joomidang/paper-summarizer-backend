package joomidang.papersummary.summary.service;

import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.paper.service.PaperService;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SummaryService {
    private final PaperService paperService;
    private final SummaryRepository summaryRepository;

    public void createSummaryFromS3(Long paperId, String s3Key) {
        log.info("✔️ 전달된 s3Key 확인: {}", s3Key);
        Paper paper = paperService.findById(paperId);
        Summary summary = Summary.builder()
                .title(paper.getTitle())
                .s3KeyMd(s3Key)
                .publishStatus(PublishStatus.DRAFT)
                .viewCount(0)
                .likeCount(0)
                .paper(paper)
                .member(paper.getMember())
                .build();
        summaryRepository.save(summary);

        log.info("요약 저장 완료 → paperId={}, summaryId={}", paperId, summary.getSummaryId());
    }
}
