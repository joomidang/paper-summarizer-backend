package joomidang.papersummary.summary.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import joomidang.papersummary.common.embedding.EmbeddingClient;
import joomidang.papersummary.summary.controller.response.SummaryListResponse;
import joomidang.papersummary.summary.controller.response.SummaryResponse;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.entity.SummaryDocument;
import joomidang.papersummary.summary.repository.SummaryElasticsearchRepository;
import joomidang.papersummary.summary.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ElasticsearchSummaryService {
    private final SummaryElasticsearchRepository elasticsearchRepository;
    private final SummaryRepository summaryRepository;
    private final EmbeddingClient embeddingClient;
    private final ElasticsearchClient elasticsearchClient;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int MIN_SEARCH_TERM_LENGTH = 2;
    private static final String RECOMMENDATION_CACHE_PREFIX = "similar_summaries:";
    private static final String EMBEDDING_CACHE_PREFIX = "embedding:";
    private static final Duration RECOMMENDATION_TTL = Duration.ofHours(6);
    private static final Duration EMBEDDING_TTL = Duration.ofDays(7);

    /**
     * 1) ES 기반 검색 메서드 - 컨트롤러/서비스에서 호출
     */
    public SummaryListResponse searchSummaries(String keyword, Pageable pageable) {
        // 1) 검색어 검증
        validateSearchTerm(keyword);

        log.info("Elasticsearch 검색 시작: keyword='{}', page={}, size={}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());

        try {
            // 2) 저장소에서 직접 검색 (findByCombinedTextContainingIgnoreCase 사용)
            Page<SummaryDocument> searchResults = elasticsearchRepository
                    .findByCombinedTextContainingIgnoreCase(keyword, pageable);

            // 3) 결과 매핑: SummaryDocument → SummaryResponse
            List<SummaryResponse> summaries = searchResults.getContent().stream()
                    .map(doc -> {
                        log.debug("검색 결과 매핑: id={}, summaryId={}", doc.getId(), doc.getSummaryId());
                        return new SummaryResponse(
                                doc.getSummaryId(),
                                doc.getTitle(),
                                doc.getBrief(),
                                null, // authorName - 필요시 추가
                                null, // authorProfileImage - 필요시 추가
                                doc.getCreatedAt(),
                                doc.getViewCount(),
                                doc.getLikeCount(),
                                0,    // commentCount - 필요시 추가
                                0.0   // popularityScore - 필요시 추가
                        );
                    })
                    .collect(Collectors.toList());

            log.info("Elasticsearch 검색 완료: keyword='{}', totalHits={}",
                    keyword, searchResults.getTotalElements());

            return new SummaryListResponse(
                    summaries,
                    searchResults.getNumber(),
                    searchResults.getTotalPages(),
                    searchResults.getTotalElements(),
                    searchResults.hasNext(),
                    searchResults.hasPrevious()
            );
        } catch (Exception e) {
            log.error("Elasticsearch 검색 중 오류 발생: {}", e.getMessage(), e);
            // 빈 결과 반환 대신 예외 처리
            throw new RuntimeException("검색 중 오류가 발생했습니다: " + e.getMessage(), e);
        }

    }

    /**
     * 유사 논문 요약본 추천 기능
     * 
     * @param summaryId 기준이 되는 요약 ID
     * @param topK 반환할 유사 요약 개수 (기본값: 5)
     * @return 유사한 요약 목록
     */
    public List<SummaryResponse> recommendSimilarSummaries(Long summaryId, int topK) {
        validateRecommendationInputs(summaryId, topK);

        // 1. 캐시 확인
        String cacheKey = RECOMMENDATION_CACHE_PREFIX + summaryId + ":" + topK;
        List<SummaryResponse> cachedResult = getCachedRecommendations(cacheKey);
        if (cachedResult != null) {
            log.info("추천 캐시 히트: summaryId={}, topK={}, size={}", summaryId, topK, cachedResult.size());
            recordCacheMetrics(true);
            return cachedResult;
        }

        recordCacheMetrics(false);
        log.info("추천 캐시 미스, 실제 검색 수행: summaryId={}, topK={}", summaryId, topK);

        try {
            // 2. 기준 문서 및 임베딩 조회 (캐시 활용)
            SummaryDocument baseDoc = findBaseSummaryDocument(summaryId);
            float[] embedding = getCachedOrFetchEmbedding(summaryId, baseDoc);

            // 3. 벡터 검색 수행
            List<SummaryResponse> recommendations = performVectorSearch(baseDoc, embedding, topK);

            // 4. 결과 캐싱
            cacheRecommendations(cacheKey, recommendations);

            log.info("추천 검색 완료 및 캐싱: summaryId={}, topK={}, resultSize={}", 
                summaryId, topK, recommendations.size());

            return recommendations;

        } catch (Exception e) {
            log.error("벡터 추천 실패, 폴백 수행: summaryId={}", summaryId, e);
            return performFallbackRecommendation(summaryId, topK);
        }
    }

    /**
     * 추천 검색 실패 시 폴백 메커니즘으로 인기 요약 반환
     */
    private List<SummaryResponse> performFallbackRecommendation(Long summaryId, int topK) {
        log.info("폴백 메커니즘 실행: 인기 요약 반환 (summaryId={}, topK={})", summaryId, topK);
        try {
            // DB에서 인기 요약 조회
            Page<Summary> popularSummaries = summaryRepository.findPopularSummariesByPublishStatus(
                    PublishStatus.PUBLISHED, 
                    org.springframework.data.domain.PageRequest.of(0, topK * 2)); // 필터링 후 충분한 결과를 얻기 위해 2배로 요청

            return popularSummaries.getContent().stream()
                    .filter(summary -> !summary.getId().equals(summaryId))
                    .limit(topK)
                    .map(summary -> new SummaryResponse(
                            summary.getId(),
                            summary.getTitle(),
                            summary.getBrief(),
                            null, // 작성자 정보 생략
                            null, // 프로필 이미지 생략
                            summary.getCreatedAt(),
                            0, // 조회수 생략
                            0, // 좋아요 수 생략
                            0, // 댓글 수 생략
                            0.0 // 인기도 점수 생략
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("폴백 메커니즘 실패: {}", e.getMessage(), e);
            return new ArrayList<>(); // 최종 폴백: 빈 목록 반환
        }
    }

    /**
     * 입력 검증 로직
     */
    private void validateRecommendationInputs(Long summaryId, int topK) {
        // 입력 검증
        if (summaryId == null || summaryId <= 0) {
            throw new IllegalArgumentException("유효한 요약 ID를 입력해주세요.");
        }
    }

    /**
     * 캐시에서 추천 결과 조회
     */
    @SuppressWarnings("unchecked")
    private List<SummaryResponse> getCachedRecommendations(String cacheKey) {
        try {
            return (List<SummaryResponse>) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("캐시 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 추천 결과를 캐시에 저장
     */
    private void cacheRecommendations(String cacheKey, List<SummaryResponse> recommendations) {
        try {
            redisTemplate.opsForValue().set(cacheKey, recommendations, RECOMMENDATION_TTL);
        } catch (Exception e) {
            log.warn("캐시 저장 실패: {}", e.getMessage());
        }
    }

    /**
     * 기준 요약 문서 조회
     */
    private SummaryDocument findBaseSummaryDocument(Long summaryId) {
        return elasticsearchRepository.findById(String.valueOf(summaryId))
                .orElseThrow(() -> new IllegalArgumentException("해당 요약을 찾을 수 없습니다: " + summaryId));
    }

    /**
     * 캐시에서 임베딩 조회 또는 문서에서 가져오기
     */
    private float[] getCachedOrFetchEmbedding(Long summaryId, SummaryDocument baseDoc) {
        // 1. 캐시에서 임베딩 조회
        String embeddingCacheKey = EMBEDDING_CACHE_PREFIX + summaryId;
        byte[] cachedEmbedding = (byte[]) redisTemplate.opsForValue().get(embeddingCacheKey);

        if (cachedEmbedding != null) {
            log.debug("임베딩 캐시 히트: summaryId={}", summaryId);
            return deserializeFloatArray(cachedEmbedding);
        }

        // 2. 캐시 미스 시 문서에서 조회
        float[] embedding = validateAndGetEmbedding(baseDoc, summaryId);

        // 3. 임베딩 캐싱
        try {
            byte[] serialized = serializeFloatArray(embedding);
            redisTemplate.opsForValue().set(embeddingCacheKey, serialized, EMBEDDING_TTL);
            log.debug("임베딩 캐싱 완료: summaryId={}", summaryId);
        } catch (Exception e) {
            log.warn("임베딩 캐싱 실패: summaryId={}, error={}", summaryId, e.getMessage());
        }

        return embedding;
    }

    /**
     * 임베딩 검증 및 가져오기
     */
    private float[] validateAndGetEmbedding(SummaryDocument baseDoc, Long summaryId) {
        float[] embedding = baseDoc.getEmbedding();
        if (embedding == null || embedding.length == 0) {
            throw new IllegalStateException("해당 요약에 임베딩 벡터가 없습니다: " + summaryId);
        }
        return embedding;
    }

    /**
     * 벡터 검색 수행
     */
    private List<SummaryResponse> performVectorSearch(SummaryDocument baseDoc, float[] embedding, int topK) {
        // topK 값 검증 및 조정
        final int finalTopK;
        if (topK <= 0) {
            finalTopK = 5; // 기본값 설정
            log.warn("유효하지 않은 topK 값({}), 기본값 5로 설정합니다.", topK);
        } else if (topK > 20) {
            finalTopK = 20; // 최대값 제한
            log.warn("topK 값이 너무 큽니다({}), 최대값 20으로 제한합니다.", topK);
        } else {
            finalTopK = topK;
        }

        // float[]를 List<Float>로 변환 (성능 최적화)
        final List<Float> vectorList = new ArrayList<>(embedding.length);
        for (float f : embedding) {
            vectorList.add(f);
        }

        try {
            // Elasticsearch KNN 검색 수행
            final int candidatesCount = Math.max(100, finalTopK * 3);
            SearchResponse<SummaryDocument> response = elasticsearchClient.search(s -> s
                            .index("summary_documents")
                            .knn(k -> k
                                    .field("embedding")
                                    .queryVector(vectorList)
                                    .k(finalTopK + 1) // 자기 자신을 제외하기 위해 1개 더 요청
                                    .numCandidates(candidatesCount) // 후보 수 최적화
                            )
                            .source(src -> src.filter(f -> f.excludes("embedding"))) // 임베딩 필드 제외하여 응답 크기 최적화
                            .query(q -> q.bool(b -> b.mustNot(m -> m
                                    .term(t -> t.field("summaryId").value(baseDoc.getSummaryId()))
                            ))),
                    SummaryDocument.class
            );

            return response.hits().hits().stream()
                    .map(hit -> {
                        SummaryDocument doc = hit.source();
                        return new SummaryResponse(
                                doc.getSummaryId(),
                                doc.getTitle(),
                                doc.getBrief(),
                                null,
                                null,
                                doc.getCreatedAt(),
                                doc.getViewCount(),
                                doc.getLikeCount(),
                                0,
                                0.0
                        );
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("벡터 검색 중 IO 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("벡터 검색 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 캐시 무효화 (요약이 업데이트되거나 삭제될 때)
     */
    public void invalidateRecommendationCache(Long summaryId) {
        try {
            Set<String> keys = redisTemplate.keys(RECOMMENDATION_CACHE_PREFIX + summaryId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("추천 캐시 무효화: summaryId={}, deletedKeys={}", summaryId, keys.size());
            }

            // 임베딩 캐시도 무효화
            String embeddingKey = EMBEDDING_CACHE_PREFIX + summaryId;
            redisTemplate.delete(embeddingKey);

        } catch (Exception e) {
            log.warn("캐시 무효화 실패: summaryId={}, error={}", summaryId, e.getMessage());
        }
    }

    /**
     * 직렬화/역직렬화 헬퍼 메서드들
     */
    private byte[] serializeFloatArray(float[] array) {
        ByteBuffer buffer = ByteBuffer.allocate(array.length * 4);
        for (float f : array) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

    private float[] deserializeFloatArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] array = new float[bytes.length / 4];
        for (int i = 0; i < array.length; i++) {
            array[i] = buffer.getFloat();
        }
        return array;
    }

    /**
     * 캐시 메트릭 기록 (모니터링용)
     */
    private void recordCacheMetrics(boolean isHit) {
        // 메트릭 수집 (모니터링용)
        log.debug("캐시 메트릭: hit={}", isHit);
    }

    public SummaryListResponse searchSummariesContaining(String keyword, Pageable pageable) {
        validateSearchTerm(keyword);

        log.info("ES Containing 검색 시작: keyword='{}', page={}, size={}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());

        Page<SummaryDocument> searchResults = elasticsearchRepository
                .findByCombinedTextContainingIgnoreCase(keyword, pageable);

        List<SummaryResponse> summaries = searchResults.getContent().stream()
                .map(doc -> new SummaryResponse(
                        doc.getSummaryId(),
                        doc.getTitle(),
                        doc.getBrief(),
                        null, // authorName - 필요시 추가
                        null, // authorProfileImage - 필요시 추가
                        doc.getCreatedAt(),
                        doc.getViewCount(),
                        doc.getLikeCount(),
                        0,    // commentCount - 필요시 추가
                        0.0   // popularityScore - 필요시 추가
                ))
                .collect(Collectors.toList());

        log.info("ES Containing 검색 완료: keyword='{}', totalHits={}",
                keyword, searchResults.getTotalElements());
        return new SummaryListResponse(
                summaries,
                searchResults.getNumber(),
                searchResults.getTotalPages(),
                searchResults.getTotalElements(),
                searchResults.hasNext(),
                searchResults.hasPrevious()
        );
    }

    /**
     * 색인 인서트/업데이트
     */
    public void indexSummary(Summary summary) {
        log.info("Summary 인덱싱 시작: summaryId={}", summary.getId());

        String documentId = String.valueOf(summary.getId());
        String combinedText = summary.getTitle() + " " + summary.getBrief();
        //TODO: 논문 요약본 내용도 같이 임베딩 하도록 변경
        String embeddingInput = "passage: " + combinedText; // E5 모델용 프리픽스

        try {
            List<Float> embedding = embeddingClient.embed(
                    "intfloat/multilingual-e5-small", embeddingInput
            );

            SummaryDocument document = SummaryDocument.builder()
                    .id(documentId) // 문자열 ID 사용
                    .summaryId(summary.getId())
                    .title(summary.getTitle())
                    .brief(summary.getBrief())
                    .combinedText(summary.getTitle() + " " + summary.getBrief())
                    .likeCount(summary.getLikeCount())
                    .viewCount(summary.getViewCount())
                    .createdAt(summary.getCreatedAt())
                    .embedding(toFloatArray(embedding))
                    .build();

            elasticsearchRepository.save(document);

            // 캐시 무효화 (요약이 업데이트되면 추천 결과도 변경될 수 있음)
            invalidateRecommendationCache(summary.getId());

            log.info("Summary 인덱싱 및 캐시 무효화 완료: summaryId={}, documentId={}", summary.getId(), documentId);
        } catch (Exception e) {
            log.error("인덱싱 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("인덱싱 중 오류가 발생했습니다: " + e.getMessage(), e);
        }

    }

    // 요약 삭제
    @Transactional
    public void deleteSummary(Long summaryId) {
        String documentId = String.valueOf(summaryId);
        log.info("Elasticsearch에서 Summary 삭제: summaryId={}, documentId={}", summaryId, documentId);
        elasticsearchRepository.deleteById(documentId);

        // 캐시 무효화
        invalidateRecommendationCache(summaryId);
        log.info("삭제된 요약의 캐시 무효화 완료: summaryId={}", summaryId);
    }

    /**
     * 모든 요약을 한 번에 색인(Bulk)
     */
    @Transactional
    public void indexAllSummaries() {
        log.info("모든 요약 Bulk 색인 시작");
        List<Summary> allSummaries = summaryRepository.findAll();
        for (Summary summary : allSummaries) {
            if (summary.getPublishStatus() == PublishStatus.PUBLISHED && !summary.isDeleted()) {
                indexSummary(summary); //TODO: 메서드 변경 되면 별도의 방법 생각
            }
        }
        log.info("모든 요약 Bulk 색인 완료");
    }


    /**
     * 검색어 검증 로직 (서비스 계층에서도 호출)
     */
    private void validateSearchTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }
        String trimmed = searchTerm.trim();
        if (trimmed.length() < MIN_SEARCH_TERM_LENGTH) {
            throw new IllegalArgumentException("검색어는 최소 " + MIN_SEARCH_TERM_LENGTH + "자 이상이어야 합니다.");
        }
    }

    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
