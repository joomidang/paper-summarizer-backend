package joomidang.papersummary.summary.repository;

import joomidang.papersummary.summary.entity.SummaryDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SummaryElasticsearchRepository extends ElasticsearchRepository<SummaryDocument, String> {
    // 1) 단순 Containing 검색 - title 또는 brief 에 keyword 포함
    Page<SummaryDocument> findByTitleContainingIgnoreCaseOrBriefContainingIgnoreCase(
            String title, String brief, Pageable pageable);

    // 2) combinedText 필드를 단일 컨테이너로 쓸 때
    Page<SummaryDocument> findByCombinedTextContainingIgnoreCase(String text, Pageable pageable);
}
