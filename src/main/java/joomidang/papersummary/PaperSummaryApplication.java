package joomidang.papersummary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(
    basePackages = "joomidang.papersummary",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {
            joomidang.papersummary.common.config.elasticsearch.repository.SummaryElasticsearchRepository.class
        }
    )
)
@EnableElasticsearchRepositories(
    basePackages = "joomidang.papersummary.common.config.elasticsearch.repository"
)
public class PaperSummaryApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaperSummaryApplication.class, args);
    }

}
