package joomidang.papersummary.common.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {
                // 각 도메인별 JPA Repository 패키지들을 명시적으로 지정
                "joomidang.papersummary.analysislog.repository",
                "joomidang.papersummary.comment.repository",
                "joomidang.papersummary.member.repository",
                "joomidang.papersummary.paper.repository",
                "joomidang.papersummary.summary.repository",
                "joomidang.papersummary.tag.repository",
                "joomidang.papersummary.visualcontent.repository"
        },
        // JPA Repository임을 명확히 하기 위한 필터
        includeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = org.springframework.data.jpa.repository.JpaRepository.class
        ),
        // Elasticsearch Repository는 제외
        excludeFilters = {
                @org.springframework.context.annotation.ComponentScan.Filter(
                        type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                        classes = org.springframework.data.elasticsearch.repository.ElasticsearchRepository.class
                ),
                @org.springframework.context.annotation.ComponentScan.Filter(
                        type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                        classes = org.springframework.data.keyvalue.repository.KeyValueRepository.class
                )
        }
)
@EntityScan(basePackages = {
        "joomidang.papersummary.analysislog.entity",
        "joomidang.papersummary.comment.entity",
        "joomidang.papersummary.member.entity",
        "joomidang.papersummary.paper.entity",
        "joomidang.papersummary.summary.entity",
        "joomidang.papersummary.tag.entity",
        "joomidang.papersummary.visualcontent.entity",
        "joomidang.papersummary.common.audit.entity"
})
public class JpaConfig {
}
