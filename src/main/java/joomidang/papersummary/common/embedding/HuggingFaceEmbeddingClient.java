package joomidang.papersummary.common.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
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
            // 1) URL 결정: sentence-transformers 계열은 router.hf-inference 엔드포인트 사용
            String url;
            if (modelId.startsWith("sentence-transformers/")) {
                url = "https://router.huggingface.co/hf-inference/models/"
                        + modelId
                        + "/pipeline/feature-extraction";
            } else {
                url = "https://api-inference.huggingface.co/models/" + modelId;
            }

            // 2) HttpHeaders 설정: Content-Type + Authorization + Accept
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            // 3) 입력 전처리: “passage:”나 “query:” 같은 prefix 제거
            String processedInput;
            if (modelId.startsWith("sentence-transformers/")) {
                processedInput = input.replace("passage: ", "").replace("query: ", "");
            } else {
                processedInput = input;
            }

            // 4) payload 구성: inputs + options
            Map<String, Object> payload = new HashMap<>();
            payload.put("inputs", processedInput);

            Map<String, Object> options = new HashMap<>();
            options.put("wait_for_model", true);
            options.put("use_cache", false);
            payload.put("options", options);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            log.debug("HuggingFace API 요청: url={}, model={}, input길이={}", url, modelId, processedInput.length());
            log.debug("요청 payload: {}", payload);

            // 5) API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("HuggingFace API 실패: status={}, body={}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("HuggingFace API 실패: " + response.getStatusCode());
            }

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                throw new RuntimeException("HuggingFace API 응답이 비어있습니다");
            }

            log.debug("API 응답 수신: 길이={}", responseBody.length());

            JsonNode responseJson = objectMapper.readTree(responseBody);
            log.debug("📥 API 원시 응답: {}", responseJson.toPrettyString());
            // 6) 응답 파싱: [[float,…]] 혹은 [float,…] 두 형태 모두 처리
            List<Float> embedding = parseEmbeddingResponse(responseJson);

            log.debug("임베딩 벡터 생성 완료: 차원 수={}", embedding.size());
            return embedding;

        } catch (Exception e) {
            log.error("HuggingFace 임베딩 실패: model={}, input={}", modelId, input, e);
            throw new RuntimeException("HuggingFace 임베딩 실패", e);
        }
    }


    /**
     * sentence-transformers 모델 응답 파싱: 1) ["java.util.ArrayList", [ … ]] 형태라면 최외곽 문자열 무시하고 그 뒤의 배열로 진입 2) 내부 배열이 또 [ [ …
     * ] ]처럼 중첩되어 있으면, 계속 첫 번째 원소만 따라 내려가 숫자 배열 부분으로 이동 3) 최종 배열 요소가 ["java.lang.Float", 숫자] 쌍이면, 숫자만 뽑아 List<Float>로
     * 반환
     */
    private List<Float> parseEmbeddingResponse(JsonNode responseJson) {
        try {
            if (!responseJson.isArray()) {
                throw new RuntimeException("응답이 배열 형태가 아닙니다: " + responseJson.getNodeType());
            }

            JsonNode current = getJsonNode(responseJson);

            // 3) 이제 current는 [ ["java.lang.Float", 숫자], ["java.lang.Float", 숫자], … ]
            //    또는 이미 [ 숫자, 숫자, … ] 형태일 수 있다.
            List<Float> embedding = new ArrayList<>();
            for (JsonNode elem : current) {
                if (elem.isArray() && elem.size() == 2 && elem.get(1).isNumber()) {
                    // ["java.lang.Float", -0.2616296] 같은 쌍에서 두 번째 값만 사용
                    embedding.add((float) elem.get(1).asDouble());
                } else if (elem.isNumber()) {
                    // 이미 [0.12345, 0.54321, …] 형태가 된 경우
                    embedding.add((float) elem.asDouble());
                } else {
                    throw new RuntimeException("벡터 요소 파싱 실패: " + elem.toString());
                }
            }

            if (embedding.isEmpty()) {
                throw new RuntimeException("임베딩 벡터가 비어 있습니다: " + responseJson.toString());
            }
            return embedding;

        } catch (Exception e) {
            log.error("임베딩 응답 파싱 실패: response={}", responseJson.toPrettyString(), e);
            throw new RuntimeException("임베딩 응답 파싱 실패", e);
        }
    }

    private JsonNode getJsonNode(JsonNode responseJson) {
        JsonNode current = responseJson;

        // 1) 최외곽 ["java.util.ArrayList", [ … ]] 형태 처리
        if (current.size() == 2
                && current.get(0).isTextual()
                && current.get(1).isArray()) {
            current = current.get(1); // 두 번째 요소(진짜 벡터 배열)로 내려간다
        }

        // 2) 내부가 배치 응답 형태 [ [ … ] ]이라면 계속 첫 번째 배열로 내려간다
        while (current.isArray()
                && current.size() > 0
                && current.get(0).isArray()) {
            current = current.get(0);
        }
        return current;
    }
}