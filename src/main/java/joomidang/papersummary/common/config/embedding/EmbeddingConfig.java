package joomidang.papersummary.common.config.embedding;

import joomidang.papersummary.common.embedding.EmbeddingClient;
import joomidang.papersummary.common.embedding.HuggingFaceEmbeddingClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Configuration
public class EmbeddingConfig {

    @Bean
    public HuggingFaceEmbeddingClient huggingFaceEmbeddingClient(@Value("${huggingface.api.key}") String apiKey,
                                           RestTemplate restTemplate) {
        return new HuggingFaceEmbeddingClient(apiKey, restTemplate);
    }

    @Bean
    @Primary
    public EmbeddingClient cachedEmbeddingClient(HuggingFaceEmbeddingClient huggingFaceEmbeddingClient) {
        return new CachedEmbeddingClient(huggingFaceEmbeddingClient);
    }

    private static class CachedEmbeddingClient implements EmbeddingClient {
        private final HuggingFaceEmbeddingClient embeddingClient;

        public CachedEmbeddingClient(HuggingFaceEmbeddingClient embeddingClient) {
            this.embeddingClient = embeddingClient;
        }

        @Override
        @Cacheable(value = "embeddings", key = "#modelId + ':' + #input.hashCode()")
        public List<Float> embed(String modelId, String input) {
            log.info("Cache miss for embedding: modelId={}, input={}", modelId, 
                    input.substring(0, Math.min(50, input.length())));
            return embeddingClient.embed(modelId, input);
        }
    }
}
