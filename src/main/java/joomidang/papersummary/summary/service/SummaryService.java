package joomidang.papersummary.summary.service;

import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.paper.service.PaperService;
import joomidang.papersummary.summary.controller.response.SummarySuccessCode;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.exception.SummaryCreationFailedException;
import joomidang.papersummary.summary.exception.SummaryNotFoundException;
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
        log.info("S3에서 요약 생성 시작: paperId={}", paperId);
        log.debug("전달된 s3Key: {}", s3Key);

        if (s3Key == null || s3Key.isBlank()) {
            log.error("S3 키가 유효하지 않음: s3Key={}", s3Key);
            throw new SummaryCreationFailedException("S3 키가 유효하지 않습니다.");
        }

        try {
            log.debug("논문 정보 조회 시작: paperId={}", paperId);
            Paper paper = paperService.findById(paperId);
            log.debug("논문 정보 조회 완료: paperId={}, title={}", paperId, paper.getTitle());

            // 이미 요약이 존재하는지 확인
            log.debug("기존 요약 존재 여부 확인: paperId={}", paperId);
            if (summaryRepository.existsByPaper(paper)) {
                log.warn("이미 요약이 존재합니다: paperId={}", paperId);
                // 기존 요약이 있으면 업데이트하는 로직을 추가할 수 있음
                return;
            }
            log.debug("기존 요약 없음, 새 요약 생성 진행");

            log.debug("요약 엔티티 생성: title={}, s3KeyMd={}, memberId={}", 
                    paper.getTitle(), s3Key, paper.getMember().getId());
            Summary summary = Summary.builder()
                    .title(paper.getTitle())
                    .s3KeyMd(s3Key)
                    .publishStatus(PublishStatus.DRAFT)
                    .viewCount(0)
                    .likeCount(0)
                    .paper(paper)
                    .member(paper.getMember())
                    .build();

            log.debug("요약 저장 시작");
            summaryRepository.save(summary);
            log.info("요약 저장 완료: paperId={}, summaryId={}", 
                    paperId, summary.getSummaryId());
        } catch (Exception e) {
            log.error("요약 생성 중 오류 발생: paperId={}, 오류={}", paperId, e.getMessage(), e);
            throw new SummaryCreationFailedException(e.getMessage());
        }
    }

    public Summary findById(Long summaryId) {
        log.debug("요약 정보 조회 시작: summaryId={}", summaryId);

        try {
            Summary summary = summaryRepository.findById(summaryId)
                    .orElseThrow(() -> {
                        log.error("요약 정보를 찾을 수 없음: summaryId={}", summaryId);
                        return new SummaryNotFoundException(summaryId);
                    });

            log.debug("요약 정보 조회 완료: summaryId={}", summaryId);
            return summary;
        } catch (SummaryNotFoundException e) {
            // 이미 로그가 기록된 예외는 다시 던짐
            throw e;
        } catch (Exception e) {
            log.error("요약 정보 조회 중 오류 발생: summaryId={}, 오류={}", summaryId, e.getMessage(), e);
            throw e;
        }
    }
}
