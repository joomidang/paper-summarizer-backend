package joomidang.papersummary.summary.service;

import java.util.List;
import java.util.stream.Collectors;
import joomidang.papersummary.summary.controller.response.SummaryListResponse;
import joomidang.papersummary.summary.controller.response.SummaryResponse;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.entity.SummaryDocument;
import joomidang.papersummary.summary.repository.SummaryElasticsearchRepository;
import joomidang.papersummary.summary.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ElasticsearchSummaryService {
    private final SummaryElasticsearchRepository elasticsearchRepository;
    private final SummaryRepository summaryRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    private static final int MIN_SEARCH_TERM_LENGTH = 2;

    /**
     * 1) ES 기반 검색 메서드 - 컨트롤러/서비스에서 호출
     */
    public SummaryListResponse searchSummaries(String keyword, Pageable pageable) {
        // 1) 검색어 검증
        validateSearchTerm(keyword);

        log.info("Elasticsearch 검색 시작: keyword='{}', page={}, size={}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());

        try {
            // 2) 저장소에서 직접 검색 (findByCombinedTextContainingIgnoreCase 사용)
            Page<SummaryDocument> searchResults = elasticsearchRepository
                    .findByCombinedTextContainingIgnoreCase(keyword, pageable);

            // 3) 결과 매핑: SummaryDocument → SummaryResponse
            List<SummaryResponse> summaries = searchResults.getContent().stream()
                    .map(doc -> {
                        log.debug("검색 결과 매핑: id={}, summaryId={}", doc.getId(), doc.getSummaryId());
                        return new SummaryResponse(
                                doc.getSummaryId(),
                                doc.getTitle(),
                                doc.getBrief(),
                                null, // authorName - 필요시 추가
                                null, // authorProfileImage - 필요시 추가
                                doc.getCreatedAt(),
                                doc.getViewCount(),
                                doc.getLikeCount(),
                                0,    // commentCount - 필요시 추가
                                0.0   // popularityScore - 필요시 추가
                        );
                    })
                    .collect(Collectors.toList());

            log.info("Elasticsearch 검색 완료: keyword='{}', totalHits={}",
                    keyword, searchResults.getTotalElements());

            return new SummaryListResponse(
                    summaries,
                    searchResults.getNumber(),
                    searchResults.getTotalPages(),
                    searchResults.getTotalElements(),
                    searchResults.hasNext(),
                    searchResults.hasPrevious()
            );
        } catch (Exception e) {
            log.error("Elasticsearch 검색 중 오류 발생: {}", e.getMessage(), e);
            // 빈 결과 반환 대신 예외 처리
            throw new RuntimeException("검색 중 오류가 발생했습니다: " + e.getMessage(), e);
        }

    }

    public SummaryListResponse searchSummariesContaining(String keyword, Pageable pageable) {
        validateSearchTerm(keyword);

        log.info("ES Containing 검색 시작: keyword='{}', page={}, size={}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());

        Page<SummaryDocument> searchResults = elasticsearchRepository
                .findByCombinedTextContainingIgnoreCase(keyword, pageable);

        List<SummaryResponse> summaries = searchResults.getContent().stream()
                .map(doc -> new SummaryResponse(
                        doc.getSummaryId(),
                        doc.getTitle(),
                        doc.getBrief(),
                        null, // authorName - 필요시 추가
                        null, // authorProfileImage - 필요시 추가
                        doc.getCreatedAt(),
                        doc.getViewCount(),
                        doc.getLikeCount(),
                        0,    // commentCount - 필요시 추가
                        0.0   // popularityScore - 필요시 추가
                ))
                .collect(Collectors.toList());

        log.info("ES Containing 검색 완료: keyword='{}', totalHits={}",
                keyword, searchResults.getTotalElements());
        return new SummaryListResponse(
                summaries,
                searchResults.getNumber(),
                searchResults.getTotalPages(),
                searchResults.getTotalElements(),
                searchResults.hasNext(),
                searchResults.hasPrevious()
        );
    }

    /**
     * 색인 인서트/업데이트
     */
    public void indexSummary(Summary summary) {
        log.info("Summary 인덱싱 시작: summaryId={}", summary.getId());

        // ID를 항상 문자열로 처리
        String documentId = String.valueOf(summary.getId());

        try {
            SummaryDocument document = SummaryDocument.builder()
                    .id(documentId) // 문자열 ID 사용
                    .summaryId(summary.getId())
                    .title(summary.getTitle())
                    .brief(summary.getBrief())
                    .combinedText(summary.getTitle() + " " + summary.getBrief())
                    .likeCount(summary.getLikeCount())
                    .viewCount(summary.getViewCount())
                    .createdAt(summary.getCreatedAt())
                    .build();

            elasticsearchRepository.save(document);
            log.info("Summary 인덱싱 완료: summaryId={}, documentId={}", summary.getId(), documentId);
        } catch (Exception e) {
            log.error("인덱싱 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("인덱싱 중 오류가 발생했습니다: " + e.getMessage(), e);
        }

    }

    // 요약 삭제
    @Transactional
    public void deleteSummary(Long summaryId) {
        String documentId = String.valueOf(summaryId);
        log.info("Elasticsearch에서 Summary 삭제: summaryId={}, documentId={}", summaryId, documentId);
        elasticsearchRepository.deleteById(documentId);
    }

    /**
     * 모든 요약을 한 번에 색인(Bulk)
     */
    @Transactional
    public void indexAllSummaries() {
        log.info("모든 요약 Bulk 색인 시작");
        List<Summary> allSummaries = summaryRepository.findAll();
        for (Summary summary : allSummaries) {
            if (summary.getPublishStatus() == PublishStatus.PUBLISHED && !summary.isDeleted()) {
                indexSummary(summary);
            }
        }
        log.info("모든 요약 Bulk 색인 완료");
    }


    /**
     * 검색어 검증 로직 (서비스 계층에서도 호출)
     */
    private void validateSearchTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }
        String trimmed = searchTerm.trim();
        if (trimmed.length() < MIN_SEARCH_TERM_LENGTH) {
            throw new IllegalArgumentException("검색어는 최소 " + MIN_SEARCH_TERM_LENGTH + "자 이상이어야 합니다.");
        }
    }
}
