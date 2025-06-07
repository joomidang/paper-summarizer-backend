package joomidang.papersummary.common.config.elasticsearch;

import joomidang.papersummary.common.config.elasticsearch.service.ElasticsearchSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch 초기화 및 인덱싱을 담당하는 컴포넌트 애플리케이션 시작 후 모든 요약본을 재인덱싱합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final ElasticsearchSummaryService elasticsearchSummaryService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("애플리케이션 시작 완료. Elasticsearch 인덱스 초기화 시작...");
        try {
            // 모든 요약본 재인덱싱
            elasticsearchSummaryService.indexAllSummaries();
            log.info("Elasticsearch 인덱스 초기화 완료");
        } catch (Exception e) {
            log.error("Elasticsearch 인덱스 초기화 중 오류 발생", e);
        }
    }
}