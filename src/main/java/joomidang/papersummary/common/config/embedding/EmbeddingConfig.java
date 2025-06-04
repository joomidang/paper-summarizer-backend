package joomidang.papersummary.common.config.embedding;

import joomidang.papersummary.common.embedding.EmbeddingClient;
import joomidang.papersummary.common.embedding.HuggingFaceEmbeddingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class EmbeddingConfig {
    @Bean
    public EmbeddingClient embeddingClient(@Value("huggingface.api.key") String apiKey,
                                           RestTemplate restTemplate) {
        return new HuggingFaceEmbeddingClient(apiKey, restTemplate);
    }
}
