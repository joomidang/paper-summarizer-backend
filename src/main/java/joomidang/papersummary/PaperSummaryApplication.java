package joomidang.papersummary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;

@SpringBootApplication(
        // Spring Data 자동 설정에서 Repository 스캔 제외
        exclude = {
                ElasticsearchRepositoriesAutoConfiguration.class,
                ReactiveElasticsearchRepositoriesAutoConfiguration.class, // Reactive Elasticsearch 자동설정 제외
                RedisRepositoriesAutoConfiguration.class
        }
)
public class PaperSummaryApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaperSummaryApplication.class, args);
    }

}
