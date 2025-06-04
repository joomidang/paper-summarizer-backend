package joomidang.papersummary.common.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
public class HuggingFaceEmbeddingClient implements EmbeddingClient {
    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Float> embed(String modelId, String input) {
        try {
            String url = "https://api-inference.huggingface.co/pipeline/feature-extraction/" + modelId;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> payload = new HashMap<>();
            payload.put("inputs", input);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("HuggingFace API 실패: " + response.getStatusCode());
            }

            JsonNode vector = objectMapper.readTree(response.getBody());
            List<Float> embedding = new ArrayList<>();
            for (JsonNode n : vector.get(0)) {
                embedding.add((float) n.asDouble());
            }
            return embedding;

        } catch (Exception e) {
            log.error("HuggingFace 임베딩 실패", e);
            throw new RuntimeException("HuggingFace 임베딩 실패", e);
        }
    }
}
