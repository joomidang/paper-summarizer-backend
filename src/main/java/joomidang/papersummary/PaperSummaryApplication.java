package joomidang.papersummary;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;

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

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> httpToHttpsRedirect() {
        return factory -> {
            Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setScheme("http");
            connector.setPort(8080); // HTTP
            connector.setSecure(false);
            connector.setRedirectPort(443); // 리디렉션 대상
            factory.addAdditionalTomcatConnectors(connector);
        };
    }
}
