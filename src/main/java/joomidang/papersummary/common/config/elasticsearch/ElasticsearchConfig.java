package joomidang.papersummary.common.config.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Slf4j
@Configuration
@EnableElasticsearchRepositories(
        basePackages = "joomidang.papersummary.common.config.elasticsearch.repository",
        // Elasticsearch Repository만 포함.
        includeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = org.springframework.data.elasticsearch.repository.ElasticsearchRepository.class
        ),
        // JPA Repository 및 다른 Repository 타입 제외
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = org.springframework.data.jpa.repository.JpaRepository.class
        )
)
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUri;

    // ElasticsearchClient를 생성자 주입 대신 메서드에서 직접 생성
    private ElasticsearchClient createElasticsearchClient() {
        // Jackson ObjectMapper 설정
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // RestClient 생성
        RestClient restClient = RestClient.builder(
                HttpHost.create(elasticsearchUri)
        ).build();

        // Jackson 매퍼가 포함된 Transport 생성
        ElasticsearchTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper(mapper)
        );

        return new ElasticsearchClient(transport);
    }

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        return createElasticsearchClient();
    }

    @Bean
    public ElasticsearchTemplate elasticsearchTemplate(ElasticsearchClient elasticsearchClient) {
        return new ElasticsearchTemplate(elasticsearchClient);
    }

    @PostConstruct
    public void createNoriAnalyzer() {
        // PostConstruct에서는 별도의 클라이언트 인스턴스 사용
        ElasticsearchClient client = createElasticsearchClient();

        try {
            // 인덱스가 존재하는지 확인
            boolean indexExists = client.indices().exists(e -> e.index("summary_documents")).value();

            // 기존 인덱스 삭제 (테스트 환경에서만 사용)
            if (indexExists) {
                client.indices().delete(d -> d.index("summary_documents"));
                log.info("기존 'summary_documents' 인덱스를 삭제했습니다.");
            }

            // 인덱스 생성 요청 구성
            client.indices().create(c -> c
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
                            .properties("publishedAt", p -> p.date(d -> d
                                    .format("strict_date_optional_time||yyyy-MM-dd HH:mm:ss||yyyy-MM-dd")
                            ))
                            .properties("embedding", p -> p
                                    .denseVector(d -> d.dims(384))
                            )
                    )
            );

            log.info("'summary_documents' 인덱스가 생성되었습니다.");
        } catch (Exception e) {
            log.error("인덱스 생성 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
