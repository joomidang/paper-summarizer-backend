package joomidang.papersummary.common.config.elasticsearch.service;

import static org.springframework.data.domain.PageRequest.of;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import joomidang.papersummary.common.config.elasticsearch.entiy.SummaryDocument;
import joomidang.papersummary.common.config.elasticsearch.repository.SummaryElasticsearchRepository;
import joomidang.papersummary.common.embedding.HuggingFaceEmbeddingClient;
import joomidang.papersummary.common.util.MarkdownChunker;
import joomidang.papersummary.common.util.VectorUtils;
import joomidang.papersummary.summary.controller.response.SummaryListResponse;
import joomidang.papersummary.summary.controller.response.SummaryResponse;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.repository.SummaryRepository;
import joomidang.papersummary.tag.service.TagService;
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
    // 저장소 및 클라이언트 의존성
    private final SummaryElasticsearchRepository elasticsearchRepository;
    private final SummaryRepository summaryRepository;
    private final HuggingFaceEmbeddingClient embeddingClient;
    private final ElasticsearchClient elasticsearchClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TagService tagService;

    // 검색 관련 상수
    private static final int MIN_SEARCH_TERM_LENGTH = 2;
    private static final String ES_INDEX_NAME = "summary_documents";
    private static final String EMBEDDING_FIELD = "embedding";
    private static final String SUMMARY_ID_FIELD = "summaryId";

    // 캐시 관련 상수
    private static final String RECOMMENDATION_CACHE_PREFIX = "similar_summaries:";
    private static final String EMBEDDING_CACHE_PREFIX = "embedding:";
    private static final Duration RECOMMENDATION_TTL = Duration.ofMinutes(30);
    private static final Duration EMBEDDING_TTL = Duration.ofDays(7);

    // 임베딩 관련 상수
    private static final String EMBEDDING_MODEL = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2";
    private static final String EMBEDDING_PREFIX = "passage: ";
    private static final int MAX_CHUNK_SIZE = 512;
    private static final int DEFAULT_SIMILAR_COUNT = 20;
    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 20;

    // 병렬 처리를 위한 스레드 풀
    private static final ExecutorService embeddingExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2));

    // 애플리케이션 종료 시 스레드 풀 정리
    @PreDestroy
    public void cleanup() {
        log.info("임베딩 스레드 풀 종료 시작");
        embeddingExecutor.shutdown();
        try {
            if (!embeddingExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                embeddingExecutor.shutdownNow();
                if (!embeddingExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.error("임베딩 스레드 풀이 완전히 종료되지 않았습니다.");
                }
            }
            log.info("임베딩 스레드 풀 종료 완료");
        } catch (InterruptedException e) {
            embeddingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            log.error("임베딩 스레드 풀 종료 중 인터럽트 발생: {}", e.getMessage());
        }
    }

    /**
     * 1) ES 기반 검색 메서드 - 컨트롤러/서비스에서 호출
     */
    public SummaryListResponse searchSummaries(String keyword, Pageable pageable) {
        return executeSearch("Elasticsearch", keyword, pageable,
                k -> elasticsearchRepository.findByCombinedTextContainingIgnoreCase(k, pageable));
    }

    /**
     * 검색 실행을 위한 공통 메서드
     *
     * @param searchType     검색 유형 (로깅용)
     * @param keyword        검색 키워드
     * @param pageable       페이징 정보
     * @param searchFunction 실제 검색을 수행할 함수
     * @return 검색 결과 응답
     */
    private SummaryListResponse executeSearch(
            String searchType,
            String keyword,
            Pageable pageable,
            Function<String, Page<SummaryDocument>> searchFunction) {

        // 1) 검색어 검증
        validateSearchTerm(keyword);

        log.info("{} 검색 시작: keyword='{}', page={}, size={}",
                searchType, keyword, pageable.getPageNumber(), pageable.getPageSize());

        try {
            // 2) 저장소에서 검색 실행
            Page<SummaryDocument> searchResults = searchFunction.apply(keyword);

            // 3) 결과 매핑: SummaryDocument → SummaryResponse
            List<SummaryResponse> summaries = searchResults.getContent().stream()
                    .map(this::mapToSummaryResponse)
                    .collect(Collectors.toList());
            ;

            log.info("{} 검색 완료: keyword='{}', totalHits={}",
                    searchType, keyword, searchResults.getTotalElements());

            // 4) 응답 생성
            return createSearchResponse(searchResults, summaries);

        } catch (Exception e) {
            log.error("{} 검색 중 오류 발생: {}", searchType, e.getMessage(), e);
            throw new RuntimeException("검색 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 단일 SummaryDocument를 SummaryResponse로 변환
     */
    private SummaryResponse mapToSummaryResponse(SummaryDocument doc) {
        log.debug("검색 결과 매핑: id={}, summaryId={}", doc.getId(), doc.getSummaryId());

        // 작성자 정보 조회
        String[] authorInfo = getAuthorInfo(doc.getSummaryId());

        return new SummaryResponse(
                doc.getSummaryId(),
                doc.getTitle(),
                doc.getBrief(),
                authorInfo[0], // authorName
                authorInfo[1], // authorProfileImage
                doc.getCreatedAt(),
                doc.getPublishedAt(),
                doc.getViewCount(),
                doc.getLikeCount(),
                0,    // commentCount - 필요시 추가
                0.0   // popularityScore - 필요시 추가
        );
    }

    private String[] getAuthorInfo(Long summaryId) {
        try {
            Summary summary = summaryRepository.findById(summaryId)
                    .orElse(null);

            if (summary != null && summary.getMember() != null) {
                return new String[]{
                        summary.getMember().getName(),
                        summary.getMember().getProfileImage()
                };
            }
        } catch (Exception e) {
            log.warn("작성자 정보 조회 실패: summaryId={}, error={}", summaryId, e.getMessage());
        }

        // 기본값 반환
        return new String[]{"Unknown", null};
    }

    /**
     * 검색 결과로부터 응답 객체 생성
     */
    private SummaryListResponse createSearchResponse(
            Page<SummaryDocument> searchResults,
            List<SummaryResponse> summaries) {

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
     * 유사 논문 요약본 추천 기능
     *
     * @param summaryId 기준이 되는 요약 ID
     * @param topK      반환할 유사 요약 개수 (기본값: 5)
     * @return 유사한 요약 목록
     */
    public List<SummaryResponse> recommendSimilarSummaries(Long summaryId, int topK) {
        // 입력 검증
        validateRecommendationInputs(summaryId);

        // topK 값 정규화
        final int normalizedTopK = normalizeTopK(topK);

        SummaryDocument baseDoc = findBaseSummaryDocument(summaryId);
        List<String> baseTags = baseDoc.getTags();
        String categoryKey = (baseTags != null && !baseTags.isEmpty()) ? baseTags.get(0) : "default";
        // 캐시 키 생성
        String cacheKey = buildRecommendationCacheKey(categoryKey, summaryId, normalizedTopK);

        // 1. 캐시 확인
        List<SummaryResponse> cachedResult = getCachedRecommendations(cacheKey);
        if (cachedResult != null) {
            log.info("추천 캐시 히트: summaryId={}, topK={}, size={}",
                    summaryId, normalizedTopK, cachedResult.size());
            recordCacheMetrics(true);
            return cachedResult;
        }

        // 캐시 미스 기록
        recordCacheMetrics(false);
        log.info("추천 캐시 미스, 실제 검색 수행: summaryId={}, topK={}", summaryId, normalizedTopK);

        // 2. 기준 문서 및 임베딩 조회 (캐시 활용)
        float[] embedding = getCachedOrFetchEmbedding(summaryId, baseDoc);
        List<SummaryResponse> recommendations;
        try {
            // 3. 벡터 검색 수행
            recommendations = performVectorSearch(baseDoc, embedding, normalizedTopK);
        } catch (Exception e) {
            log.error("벡터 추천 실패, 폴백 수행: summaryId={}, error={}", summaryId, e.getMessage());
            recommendations = performFallbackRecommendation(summaryId, normalizedTopK);
        }
        // 4. 결과 캐싱
        log.info("추천 검색 완료 및 캐싱: summaryId={}, topK={}, resultSize={}",
                summaryId, normalizedTopK, recommendations.size());
        cacheRecommendations(cacheKey, recommendations);
        return recommendations;
    }

    /**
     * 추천 캐시 키 생성
     */
    private String buildRecommendationCacheKey(String category, Long summaryId, int topK) {
        return RECOMMENDATION_CACHE_PREFIX + category + ":" + summaryId + ":" + topK;
    }

    /**
     * topK 값 정규화 (유효 범위 내로 조정)
     */
    private int normalizeTopK(int topK) {
        if (topK <= 0) {
            log.warn("유효하지 않은 topK 값({}), 기본값 {}로 설정합니다.", topK, DEFAULT_TOP_K);
            return DEFAULT_TOP_K;
        }
        if (topK > MAX_TOP_K) {
            log.warn("topK 값이 너무 큽니다({}), 최대값 {}으로 제한합니다.", topK, MAX_TOP_K);
            return MAX_TOP_K;
        }
        return topK;
    }

    /**
     * 추천 검색 실패 시 폴백 메커니즘으로 인기 요약 반환
     */
    private List<SummaryResponse> performFallbackRecommendation(Long summaryId, int topK) {
        log.info("폴백 메커니즘 실행: 인기 요약 반환 (summaryId={}, topK={})", summaryId, topK);

        // DB에서 인기 요약 조회
        Page<Summary> popularSummaries = summaryRepository.findPopularSummariesByPublishStatus(
                PublishStatus.PUBLISHED,
                of(0, topK * 2)); // 필터링 후 충분한 결과를 얻기 위해 2배로 요청

        return popularSummaries.getContent().stream()
                .filter(summary -> !summary.getId().equals(summaryId))
                .limit(topK)
                .map(summary -> new SummaryResponse(
                        summary.getId(),
                        summary.getTitle(),
                        summary.getBrief(),
                        summary.getMember() != null ? summary.getMember().getName() : "Unknown", // 작성자 이름
                        summary.getMember() != null ? summary.getMember().getProfileImage() : null, // 프로필 이미지
                        summary.getCreatedAt(),
                        summary.getUpdatedAt(),
                        0, // 조회수 생략
                        0, // 좋아요 수 생략
                        0, // 댓글 수 생략
                        0.0 // 인기도 점수 생략
                ))
                .collect(Collectors.toList());
    }

    /**
     * 요약 ID 검증 로직
     */
    private void validateRecommendationInputs(Long summaryId) {
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
        String embeddingCacheKey = EMBEDDING_CACHE_PREFIX + summaryId;
        Object cached = redisTemplate.opsForValue().get(embeddingCacheKey);

        // 캐시된 데이터가 이미 float[]인 경우
        if (cached instanceof float[]) {
            return (float[]) cached;
        }
        if (cached != null) {
            log.debug("임베딩 캐시 히트: summaryId={}", summaryId);
            try {
                // Jackson 직렬화로 인해 타입이 변경된 경우
                return VectorUtils.toFloatArray(cached);
            } catch (Exception e) {
                log.warn("임베딩 역직렬화 실패, 캐시 제거 후 재생성: summaryId={}, error={}",
                        summaryId, e.getMessage());
                redisTemplate.delete(embeddingCacheKey); // 잘못된 캐시 제거
            }
        }

        // 캐시 미스 시 문서에서 조회
        float[] embedding = validateAndGetEmbedding(baseDoc, summaryId);

        try {
            // float[] 형태로 직접 캐싱
            redisTemplate.opsForValue().set(embeddingCacheKey, embedding, EMBEDDING_TTL);
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
     *
     * @param baseDoc   기준 문서
     * @param embedding 임베딩 벡터
     * @param topK      반환할 결과 수
     * @return 유사한 요약 목록
     */
    private List<SummaryResponse> performVectorSearch(SummaryDocument baseDoc, float[] embedding, int topK) {
        try {
            // Elasticsearch KNN 검색 수행
            final int candidatesCount = Math.max(100, topK * 3);

            // 최적화된 방식으로 float[]를 List<Float>로 변환
            List<Float> queryVector = convertToQueryVector(embedding);

            // 검색 실행
            SearchResponse<SummaryDocument> response = executeKnnSearch(
                    baseDoc,
                    queryVector,
                    topK,
                    candidatesCount
            );

            // 결과 매핑
            List<SummaryResponse> results = mapSearchHitsToResponses(response);

            // 제목 키워드 매칭 기반 재정렬
            return reRankByTitleKeywordMatches(baseDoc, results);

        } catch (IOException e) {
            log.error("벡터 검색 중 IO 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("벡터 검색 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private record Ranking(SummaryResponse response, int matchCount, int originalIndex) {
    }

    /**
     * 제목 키워드 매칭 기반 재정렬 벡터 유사도가 비슷한 경우 제목 키워드 매칭이 더 많은 문서를 우선 순위로 함
     */
    private List<SummaryResponse> reRankByTitleKeywordMatches(SummaryDocument baseDoc, List<SummaryResponse> results) {
        if (results.size() <= 1) {
            return results;
        }

        // 기준 문서의 제목에서 키워드 추출 (간단한 토큰화)
        String[] baseKeywords = baseDoc.getTitle().toLowerCase().split("[^가-힣a-zA-Z0-9]+");

        // 각 결과에 키워드 매칭 점수 계산
        List<Ranking> list = IntStream.range(0, results.size())
                .mapToObj(i -> {
                    SummaryResponse r = results.get(i);
                    // 제목 토큰화
                    String title = r.title().toLowerCase();
                    // 키워드 매칭 점수 계산
                    int count = 0;
                    for (String kw : baseKeywords) {
                        if (kw.length() > 2 && title.contains(kw)) {
                            count++;
                        }
                    }
                    // 원본 순서와 키워드 매칭 점수를 함께 고려하여 정렬
                    return new Ranking(r, count, i);
                })
                .sorted((a, b) -> {
                    // 매칭 점수가 같으면 원래 순서 유지 (안정적 정렬)
                    if (b.matchCount != a.matchCount) {
                        return Integer.compare(b.matchCount, a.matchCount);
                    }
                    // 매칭 점수가 높은 순으로 정렬
                    return Integer.compare(a.originalIndex, b.originalIndex);
                })
                .toList();
        return list.stream().map(Ranking::response).collect(Collectors.toList());
    }

    /**
     * float[] 배열을 List<Float>로 효율적으로 변환
     */
    private List<Float> convertToQueryVector(float[] embedding) {
        // 미리 크기가 지정된 ArrayList 생성으로 재할당 방지
        List<Float> vectorList = new ArrayList<>(embedding.length);
        for (float f : embedding) {
            vectorList.add(f);
        }
        return vectorList;
    }

    /**
     * Elasticsearch KNN 검색 실행
     */
    private SearchResponse<SummaryDocument> executeKnnSearch(
            SummaryDocument baseDoc,
            List<Float> queryVector,
            int topK,
            int candidatesCount) throws IOException {

        // 기준 문서의 태그 목록 조회
        List<String> baseTags = baseDoc.getTags();

        // 태그가 없는 경우 기본 KNN 검색 수행
        if (baseTags == null || baseTags.isEmpty()) {
            return elasticsearchClient.search(s -> s
                            .index(ES_INDEX_NAME)
                            .size(topK)
                            .knn(k -> k
                                    .field(EMBEDDING_FIELD)
                                    .queryVector(queryVector)
                                    .k(topK)
                                    .numCandidates(candidatesCount)
                                    .filter(f -> f.bool(b -> b.mustNot(m -> m
                                            .term(t -> t.field(SUMMARY_ID_FIELD).value(baseDoc.getSummaryId()))
                                    ))))
                            .source(src -> src.filter(f -> f.excludes(EMBEDDING_FIELD))) // 임베딩 필드 제외하여 응답 크기 최적화
                    ,
                    SummaryDocument.class
            );
        }

        // 태그가 있는 경우 KNN 검색에 태그 필터 추가
        return elasticsearchClient.search(s -> s
                        .index(ES_INDEX_NAME)
                        .size(topK)
                        .knn(k -> k
                                .field(EMBEDDING_FIELD)
                                .queryVector(queryVector)
                                .k(topK * 2)
                                .numCandidates(candidatesCount * 2)
                                .filter(f -> f.bool(b -> {
                                    b.mustNot(m -> m.term(t -> t.field(SUMMARY_ID_FIELD).value(baseDoc.getSummaryId())));
                                    for (String tag : baseTags) {
                                        b.should(s2 -> s2.term(t2 -> t2.field("tags.keyword").value(tag)));
                                    }
                                    b.minimumShouldMatch("1");
                                    return b;
                                }))
                        )
                        .source(src -> src.filter(f -> f.excludes(EMBEDDING_FIELD)))
                , SummaryDocument.class);
    }

    /**
     * 검색 결과를 SummaryResponse 목록으로 변환
     */
    private List<SummaryResponse> mapSearchHitsToResponses(SearchResponse<SummaryDocument> response) {
        return response.hits().hits().stream()
                .map(h -> mapToSummaryResponse(h.source()))
                .collect(Collectors.toList());
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
     * 모든 추천 캐시 무효화
     */
    public void invalidateAllRecommendationCaches() {
        try {
            Set<String> keys = redisTemplate.keys(RECOMMENDATION_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("모든 추천 캐시 무효화: deletedKeys={}", keys.size());
            }
        } catch (Exception e) {
            log.warn("모든 캐시 무효화 실패: error={}", e.getMessage());
        }
    }

    /**
     * 유사 요약본에 대한 선택적 캐시 무효화 새로 발행된 요약본과 유사한 요약본들의 캐시만 무효화
     */
    public void invalidateSimilarSummariesCache(Long newSummaryId) {
        log.info("유사 요약본 선택적 캐시 무효화 시작: newSummaryId={}", newSummaryId);

        try {
            // 1. 새 요약본 문서 조회
            SummaryDocument newDoc = findBaseSummaryDocument(newSummaryId);
            float[] embedding = validateAndGetEmbedding(newDoc, newSummaryId);

            // 2. 유사한 요약본 검색 (topK를 더 크게 설정하여 더 많은 유사 요약본 찾기)
            final int similarCount = 20; // 유사 요약본 개수
            List<SummaryResponse> similarSummaries = performVectorSearch(newDoc, embedding, similarCount);

            // 3. 유사 요약본들의 캐시 무효화
            for (SummaryResponse similar : similarSummaries) {
                invalidateRecommendationCache(similar.summaryId());
                log.debug("유사 요약본 캐시 무효화: similarSummaryId={}", similar.summaryId());
            }

            // 4. 새 요약본 자신의 캐시도 무효화
            invalidateRecommendationCache(newSummaryId);

            log.info("유사 요약본 선택적 캐시 무효화 완료: newSummaryId={}, invalidatedCount={}",
                    newSummaryId, similarSummaries.size() + 1);

        } catch (Exception e) {
            log.error("유사 요약본 캐시 무효화 실패, 전체 캐시 무효화로 대체: newSummaryId={}, error={}",
                    newSummaryId, e.getMessage());
            // 실패 시 안전하게 모든 캐시 무효화
            invalidateAllRecommendationCaches();
        }
    }

    /**
     * 캐시 메트릭 기록 (모니터링용)
     */
    private void recordCacheMetrics(boolean isHit) {
        // 메트릭 수집 (모니터링용)
        log.debug("캐시 메트릭: hit={}", isHit);
    }

    /**
     * 텍스트 포함 검색 메서드
     */
    public SummaryListResponse searchSummariesContaining(String keyword, Pageable pageable) {
        return executeSearch("ES Containing", keyword, pageable,
                k -> elasticsearchRepository.findByCombinedTextContainingIgnoreCase(k, pageable));
    }

    /**
     * 색인 인서트/업데이트
     *
     * @param summary         요약 객체
     * @param markdownContent 마크다운 내용 (null이면 제목과 요약만 사용)
     */
    @Transactional
    public void indexSummary(Summary summary, String markdownContent) {
        Long summaryId = summary.getId();
        log.info("Summary 인덱싱 시작: summaryId={}", summaryId);

        try {
            // 1. 임베딩 벡터 생성
            float[] embeddingVector = generateEmbeddingVector(summary, markdownContent);

            // 2. Elasticsearch 문서 생성 및 저장
            SummaryDocument document = createSummaryDocument(summary, embeddingVector);
            elasticsearchRepository.save(document);

            // 3. 선택적 캐시 무효화 (새 요약본과 유사한 요약본들의 캐시만 무효화)
            invalidateSimilarSummariesCache(summaryId);

            log.info("Summary 인덱싱 및 선택적 캐시 무효화 완료: summaryId={}, vectorDim={}",
                    summaryId, embeddingVector.length);

        } catch (Exception e) {
            log.error("인덱싱 중 오류 발생: summaryId={}, error={}", summaryId, e.getMessage(), e);
            throw new RuntimeException("인덱싱 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 요약 객체와 마크다운 내용으로부터 임베딩 벡터 생성
     */
    private float[] generateEmbeddingVector(Summary summary, String markdownContent) {
        // 마크다운 내용이 있으면 청킹하여 임베딩
        if (markdownContent != null && !markdownContent.trim().isEmpty()) {
            return generateEmbeddingFromMarkdown(summary, markdownContent);
        } else {
            // 마크다운 내용이 없으면 제목과 요약만 사용
            return generateEmbeddingFromSummaryOnly(summary);
        }
    }

    /**
     * 마크다운 내용을 청킹하여 임베딩 벡터 생성
     */
    private float[] generateEmbeddingFromMarkdown(Summary summary, String markdownContent) {
        Long summaryId = summary.getId();
        log.info("마크다운 내용 청킹 및 임베딩 시작: summaryId={}", summaryId);

        // 1. 마크다운을 섹션 단위로 나누기
        List<String> chunks = MarkdownChunker.chunkBySection(markdownContent, MAX_CHUNK_SIZE);
        log.debug("마크다운 청킹 완료: summaryId={}, chunks={}", summaryId, chunks.size());

        // 2. 각 청크를 임베딩
        List<float[]> vectors = embedChunks(summaryId, chunks);

        if (vectors.isEmpty()) {
            throw new RuntimeException("모든 청크 임베딩이 실패했습니다.");
        }

        log.debug("청크 임베딩 완료: summaryId={}, totalVectors={}", summaryId, vectors.size());

        // 3. 평균 벡터 계산
        float[] avgVector = VectorUtils.average(vectors);
        log.debug("평균 벡터 계산 완료: summaryId={}, vectorDim={}", summaryId, avgVector.length);

        return avgVector;
    }

    /**
     * 청크 목록을 임베딩하여 벡터 목록 반환 (병렬 처리)
     */
    private List<float[]> embedChunks(Long summaryId, List<String> chunks) {
        if (chunks.isEmpty()) {
            return Collections.emptyList();
        }

        // 작은 청크 수는 순차 처리가 더 효율적
        if (chunks.size() <= 3) {
            return embedChunksSequentially(summaryId, chunks);
        }

        log.debug("병렬 임베딩 시작: summaryId={}, chunks={}", summaryId, chunks.size());

        // 병렬 처리를 위한 Future 목록
        List<CompletableFuture<float[]>> futures = new ArrayList<>(chunks.size());

        // 각 청크를 병렬로 임베딩
        for (int i = 0; i < chunks.size(); i++) {
            final int chunkIndex = i;
            final String chunk = chunks.get(i);

            CompletableFuture<float[]> future = CompletableFuture.supplyAsync(() -> {
                try {
                    String embeddingInput = EMBEDDING_PREFIX + chunk;

                    // 임베딩 API 호출
                    List<Float> embeddingResult = embeddingClient.embed(
                            EMBEDDING_MODEL,
                            embeddingInput
                    );

                    // 안전한 타입 변환
                    float[] vector = VectorUtils.toFloatArray(embeddingResult);

                    log.debug("청크 임베딩 성공 (병렬): summaryId={}, chunkIndex={}, vectorDim={}",
                            summaryId, chunkIndex, vector.length);

                    return vector;
                } catch (Exception e) {
                    log.error("청크 임베딩 실패 (병렬): summaryId={}, chunkIndex={}, error={}",
                            summaryId, chunkIndex, e.getMessage(), e);
                    return null; // 실패한 경우 null 반환
                }
            }, embeddingExecutor);

            futures.add(future);
        }

        // 모든 Future 결과 수집
        List<float[]> vectors = futures.stream()
                .map(future -> {
                    try {
                        return future.join(); // 결과 대기
                    } catch (Exception e) {
                        log.error("임베딩 Future 처리 실패: {}", e.getMessage(), e);
                        return null;
                    }
                })
                .filter(vector -> vector != null) // null 결과 필터링
                .collect(Collectors.toList());

        log.debug("병렬 임베딩 완료: summaryId={}, 성공={}/{}",
                summaryId, vectors.size(), chunks.size());

        return vectors;
    }

    /**
     * 청크 목록을 순차적으로 임베딩 (적은 수의 청크용)
     */
    private List<float[]> embedChunksSequentially(Long summaryId, List<String> chunks) {
        List<float[]> vectors = new ArrayList<>(chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            try {
                String chunk = chunks.get(i);
                String embeddingInput = EMBEDDING_PREFIX + chunk;

                // 임베딩 API 호출
                List<Float> embeddingResult = embeddingClient.embed(
                        EMBEDDING_MODEL,
                        embeddingInput
                );

                // 안전한 타입 변환
                float[] vector = VectorUtils.toFloatArray(embeddingResult);
                vectors.add(vector);

                log.debug("청크 임베딩 성공 (순차): summaryId={}, chunkIndex={}, vectorDim={}",
                        summaryId, i, vector.length);

            } catch (Exception e) {
                log.error("청크 임베딩 실패 (순차): summaryId={}, chunkIndex={}, error={}",
                        summaryId, i, e.getMessage(), e);
                // 개별 청크 실패는 무시하고 계속 진행
            }
        }

        return vectors;
    }

    /**
     * 요약 정보만으로 임베딩 벡터 생성
     */
    private float[] generateEmbeddingFromSummaryOnly(Summary summary) {
        Long summaryId = summary.getId();
        log.info("마크다운 내용 없음, 제목과 요약만 임베딩: summaryId={}", summaryId);

        try {
            String combinedText = summary.getTitle() + " " + summary.getBrief();
            String embeddingInput = EMBEDDING_PREFIX + combinedText;

            Object embeddingResult = embeddingClient.embed(
                    EMBEDDING_MODEL,
                    embeddingInput
            );

            return VectorUtils.toFloatArray(embeddingResult);

        } catch (Exception e) {
            log.error("기본 임베딩 실패: summaryId={}, error={}", summaryId, e.getMessage(), e);
            throw new RuntimeException("기본 임베딩 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 요약 객체와 임베딩 벡터로부터 Elasticsearch 문서 생성
     */
    private SummaryDocument createSummaryDocument(Summary summary, float[] embeddingVector) {
        // 요약본의 태그 목록 조회
        List<String> tagNames = tagService.getTagNamesBySummary(summary.getId());

        return SummaryDocument.builder()
                .id(summary.getId())
                .summaryId(summary.getId())
                .title(summary.getTitle())
                .brief(summary.getBrief())
                .combinedText(summary.getTitle() + " " + summary.getBrief())
                .likeCount(summary.getLikeCount())
                .viewCount(summary.getViewCount())
                .createdAt(summary.getCreatedAt())
                .publishedAt(summary.getUpdatedAt())
                .embedding(embeddingVector)
                .tags(tagNames)
                .build();
    }

    /**
     * 색인 인서트/업데이트 (마크다운 내용 없이) 하위 호환성을 위한 메서드
     */
    @Transactional
    public void indexSummary(Summary summary) {
        indexSummary(summary, null);
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
     * 모든 요약을 한 번에 색인(Bulk) 참고: 마크다운 내용은 S3에서 가져와야 하므로 여기서는 제목과 요약만 사용하는 버전 호출
     */
    @Transactional
    public void indexAllSummaries() {
        log.info("모든 요약 Bulk 색인 시작");
        List<Summary> allSummaries = summaryRepository.findAll();
        for (Summary summary : allSummaries) {
            if (summary.getPublishStatus() == PublishStatus.PUBLISHED && !summary.isDeleted()) {
                // 마크다운 내용 없이 기본 인덱싱 수행 (제목과 요약만 사용)
                indexSummary(summary);
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
}
