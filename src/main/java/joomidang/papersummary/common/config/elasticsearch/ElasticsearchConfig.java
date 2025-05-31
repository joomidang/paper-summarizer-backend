package joomidang.papersummary.common.config.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Slf4j
@Configuration
@EnableElasticsearchRepositories(basePackages = "joomidang.papersummary.summary.repository")
public class ElasticsearchConfig {

    private final ElasticsearchClient elasticsearchClient;

    public ElasticsearchConfig(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @Bean
    public ElasticsearchTemplate elasticsearchTemplate() {
        return new ElasticsearchTemplate(elasticsearchClient);
    }

    @PostConstruct
    public void createNoriAnalyzer() {
        try {
            // 인덱스가 존재하는지 확인
            boolean indexExists = elasticsearchClient.indices().exists(e -> e.index("summary_documents")).value();

            // 기존 인덱스 삭제 (테스트 환경에서만 사용)
            if (indexExists) {
                elasticsearchClient.indices().delete(d -> d.index("summary_documents"));
                log.info("기존 'summary_documents' 인덱스를 삭제했습니다.");
                indexExists = false;
            }

            if (!indexExists) {
                // 인덱스 생성 요청 구성
                elasticsearchClient.indices().create(c -> c
                        .index("summary_documents")
                        .settings(s -> s
                                .analysis(a -> a
                                        .analyzer("korean", analyzer -> analyzer
                                                .custom(custom -> custom
                                                        .tokenizer("standard")
                                                        .filter("lowercase")
                                                )
                                        )
                                )
                        )
                        .mappings(m -> m
                                // _id 필드 정의 제거 - 이미 시스템 필드임
                                .properties("id", p -> p.keyword(k -> k))
                                .properties("title", p -> p.text(t -> t.analyzer("korean")))
                                .properties("brief", p -> p.text(t -> t.analyzer("korean")))
                                .properties("combinedText", p -> p.text(t -> t.analyzer("korean")))
                                .properties("summaryId", p -> p.keyword(k -> k))
                                .properties("likeCount", p -> p.integer(i -> i))
                                .properties("viewCount", p -> p.integer(i -> i))
                                .properties("createdAt", p -> p.date(d -> d
                                        .format("strict_date_optional_time||yyyy-MM-dd HH:mm:ss||yyyy-MM-dd")
                                ))
                        )
                );

                log.info("'summary_documents' 인덱스가 생성되었습니다.");
            }
        } catch (Exception e) {
            log.error("인덱스 생성 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}