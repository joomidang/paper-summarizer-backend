package joomidang.papersummary.summary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import joomidang.papersummary.common.config.elasticsearch.entiy.SummaryDocument;
import joomidang.papersummary.common.config.elasticsearch.repository.SummaryElasticsearchRepository;
import joomidang.papersummary.common.config.elasticsearch.service.ElasticsearchSummaryService;
import joomidang.papersummary.common.embedding.HuggingFaceEmbeddingClient;
import joomidang.papersummary.summary.controller.response.SummaryResponse;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.repository.SummaryRepository;
import joomidang.papersummary.tag.service.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
public class ElasticsearchSummaryServiceMockTest {

    @Mock
    private SummaryElasticsearchRepository elasticsearchRepository;

    @Mock
    private SummaryRepository summaryRepository;

    @Mock
    private HuggingFaceEmbeddingClient embeddingClient;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private TagService tagService;

    @InjectMocks
    private ElasticsearchSummaryService elasticsearchSummaryService;

    private SummaryDocument baseSummaryDocument;
    private SummaryDocument similarSummaryDocument1;
    private SummaryDocument similarSummaryDocument2;
    private Summary popularSummary1;
    private Summary popularSummary2;

    @BeforeEach
    void setUp() {
        // Redis 모킹 설정 - lenient()를 사용하여 불필요한 스텁 경고 방지
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 테스트용 임베딩 벡터 생성 (384차원)
        float[] baseEmbedding = createTestEmbedding(0.1f);
        float[] similarEmbedding1 = createTestEmbedding(0.2f);
        float[] similarEmbedding2 = createTestEmbedding(0.3f);

        // 태그 목록 생성
        List<String> baseTags = Arrays.asList("인공지능", "AI", "논문");
        List<String> similarTags1 = Arrays.asList("딥러닝", "인공지능", "AI");
        List<String> similarTags2 = Arrays.asList("머신러닝", "ML", "기술동향");

        // 기준 요약본 문서
        baseSummaryDocument = SummaryDocument.builder()
                .id(1L)
                .summaryId(1L)
                .title("인공지능 논문 요약")
                .brief("인공지능 관련 논문 요약입니다.")
                .combinedText("인공지능 논문 요약 인공지능 관련 논문 요약입니다.")
                .likeCount(20)
                .viewCount(100)
                .createdAt(LocalDateTime.now())
                .embedding(baseEmbedding)
                .tags(baseTags)
                .build();

        // 유사한 요약본 문서 1
        similarSummaryDocument1 = SummaryDocument.builder()
                .id(2L)
                .summaryId(2L)
                .title("딥러닝과 인공지능")
                .brief("딥러닝과 인공지능에 관한 요약입니다.")
                .combinedText("딥러닝과 인공지능 딥러닝과 인공지능에 관한 요약입니다.")
                .likeCount(15)
                .viewCount(80)
                .createdAt(LocalDateTime.now())
                .embedding(similarEmbedding1)
                .tags(similarTags1)
                .build();

        // 유사한 요약본 문서 2
        similarSummaryDocument2 = SummaryDocument.builder()
                .id(3L)
                .summaryId(3L)
                .title("머신러닝 기술 동향")
                .brief("최신 머신러닝 기술 동향에 관한 요약입니다.")
                .combinedText("머신러닝 기술 동향 최신 머신러닝 기술 동향에 관한 요약입니다.")
                .likeCount(10)
                .viewCount(50)
                .createdAt(LocalDateTime.now())
                .embedding(similarEmbedding2)
                .tags(similarTags2)
                .build();

        // TagService 모킹 설정
        when(tagService.getTagNamesBySummary(1L)).thenReturn(baseTags);
        when(tagService.getTagNamesBySummary(2L)).thenReturn(similarTags1);
        when(tagService.getTagNamesBySummary(3L)).thenReturn(similarTags2);

        // 인기 요약본 1 (폴백 메커니즘 테스트용)
        popularSummary1 = Summary.builder()
                .id(4L)
                .title("인기 요약본 1")
                .brief("인기 있는 요약본 1입니다.")
                .publishStatus(PublishStatus.PUBLISHED)
                .s3KeyMd("dummy-s3-key-1") // 필수 필드 추가
                .build();

        // 인기 요약본 2 (폴백 메커니즘 테스트용)
        popularSummary2 = Summary.builder()
                .id(5L)
                .title("인기 요약본 2")
                .brief("인기 있는 요약본 2입니다.")
                .publishStatus(PublishStatus.PUBLISHED)
                .s3KeyMd("dummy-s3-key-2") // 필수 필드 추가
                .build();
    }

    /**
     * 테스트용 임베딩 벡터 생성 (384차원)
     *
     * @param baseValue 벡터 값의 기본 값
     * @return 384차원의 임베딩 벡터
     */
    private float[] createTestEmbedding(float baseValue) {
        float[] embedding = new float[384]; // Hugging Face 모델 차원 수
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = baseValue + (i * 0.001f); // 각 차원마다 약간씩 다른 값 설정
        }
        return embedding;
    }

    @Test
    @DisplayName("유사한 요약본 추천 테스트 - 정상 케이스 및 캐싱")
    void recommendSimilarSummariesTest() throws Exception {
        // given
        Long summaryId = 1L;
        int topK = 2;
        String cacheKey = "similar_summaries:" + summaryId + ":" + topK;

        // Mock the cache to return null (cache miss)
        when(valueOperations.get(cacheKey)).thenReturn(null);

        // Mock the repository to return the base summary document
        when(elasticsearchRepository.findById(String.valueOf(summaryId)))
                .thenReturn(Optional.of(baseSummaryDocument));

        // Mock the search response
        SearchResponse<SummaryDocument> mockResponse = mock(SearchResponse.class);
        HitsMetadata<SummaryDocument> mockHits = mock(HitsMetadata.class);

        // Create mock hits
        Hit<SummaryDocument> hit1 = mock(Hit.class);
        when(hit1.source()).thenReturn(similarSummaryDocument1);

        Hit<SummaryDocument> hit2 = mock(Hit.class);
        when(hit2.source()).thenReturn(similarSummaryDocument2);

        List<Hit<SummaryDocument>> hitsList = Arrays.asList(hit1, hit2);

        // Set up the mock response
        when(mockHits.hits()).thenReturn(hitsList);
        when(mockResponse.hits()).thenReturn(mockHits);

        // Mock the elasticsearch client search method
        when(elasticsearchClient.search(any(Function.class), eq(SummaryDocument.class)))
                .thenReturn(mockResponse);

        // Mock embedding cache
        when(valueOperations.get(eq("embedding:" + summaryId))).thenReturn(null);

        // when - first call (cache miss)
        List<SummaryResponse> result = elasticsearchSummaryService.recommendSimilarSummaries(summaryId, topK);

        // then
        assertNotNull(result, "결과가 null입니다.");
        assertEquals(2, result.size(), "결과 개수가 예상과 다릅니다.");

        // 첫 번째 결과 확인
        assertEquals(2L, result.get(0).summaryId(), "첫 번째 결과의 ID가 예상과 다릅니다.");
        assertEquals("딥러닝과 인공지능", result.get(0).title(), "첫 번째 결과의 제목이 예상과 다릅니다.");

        // 두 번째 결과 확인
        assertEquals(3L, result.get(1).summaryId(), "두 번째 결과의 ID가 예상과 다릅니다.");
        assertEquals("머신러닝 기술 동향", result.get(1).title(), "두 번째 결과의 제목이 예상과 다릅니다.");

        // Verify that the result was cached
        verify(valueOperations).set(eq(cacheKey), any(List.class), any(Duration.class));

        // Mock the cache to return the cached result (cache hit)
        when(valueOperations.get(cacheKey)).thenReturn(result);

        // when - second call (cache hit)
        List<SummaryResponse> cachedResult = elasticsearchSummaryService.recommendSimilarSummaries(summaryId, topK);

        // then
        assertNotNull(cachedResult, "캐시된 결과가 null입니다.");
        assertEquals(2, cachedResult.size(), "캐시된 결과 개수가 예상과 다릅니다.");

        // Verify that the elasticsearch client was not called again
        verify(elasticsearchClient, times(1)).search(any(Function.class), eq(SummaryDocument.class));
    }

    @Test
    @DisplayName("유사한 요약본 추천 테스트 - 요약본이 존재하지 않는 경우 (폴백 메커니즘)")
    void recommendSimilarSummariesNotFoundTest() {
        // given
        Long nonExistentSummaryId = 999L;
        int topK = 2;

        // Mock the repository to return empty for the non-existent summary
        when(elasticsearchRepository.findById(String.valueOf(nonExistentSummaryId)))
                .thenReturn(Optional.empty());

        // Mock the fallback mechanism (popular summaries)
        List<Summary> popularSummaries = Arrays.asList(popularSummary1, popularSummary2);
        PageImpl<Summary> popularSummariesPage = new PageImpl<>(popularSummaries);

        when(summaryRepository.findPopularSummariesByPublishStatus(
                eq(PublishStatus.PUBLISHED),
                any(Pageable.class)))
                .thenReturn(popularSummariesPage);

        // when
        List<SummaryResponse> result = elasticsearchSummaryService.recommendSimilarSummaries(nonExistentSummaryId,
                topK);

        // then
        assertNotNull(result, "폴백 결과가 null입니다.");
        assertEquals(2, result.size(), "폴백 결과 개수가 예상과 다릅니다.");

        // 폴백 메커니즘이 호출되었는지 확인
        verify(summaryRepository, times(1)).findPopularSummariesByPublishStatus(
                eq(PublishStatus.PUBLISHED),
                any(Pageable.class));
    }

    @Test
    @DisplayName("유사한 요약본 추천 테스트 - 임베딩이 없는 경우 (폴백 메커니즘)")
    void recommendSimilarSummariesNoEmbeddingTest() {
        // given
        Long summaryId = 1L;
        int topK = 2;

        // 임베딩이 없는 요약본 문서 생성
        SummaryDocument noEmbeddingDocument = SummaryDocument.builder()
                .id(1L)
                .summaryId(1L)
                .title("인공지능 논문 요약")
                .brief("인공지능 관련 논문 요약입니다.")
                .combinedText("인공지능 논문 요약 인공지능 관련 논문 요약입니다.")
                .likeCount(20)
                .viewCount(100)
                .createdAt(LocalDateTime.now())
                .embedding(null) // 임베딩 없음
                .build();

        // Mock the repository to return the document without embedding
        when(elasticsearchRepository.findById(String.valueOf(summaryId)))
                .thenReturn(Optional.of(noEmbeddingDocument));

        // Mock the fallback mechanism (popular summaries)
        List<Summary> popularSummaries = Arrays.asList(popularSummary1, popularSummary2);
        PageImpl<Summary> popularSummariesPage = new PageImpl<>(popularSummaries);

        when(summaryRepository.findPopularSummariesByPublishStatus(
                eq(PublishStatus.PUBLISHED),
                any(Pageable.class)))
                .thenReturn(popularSummariesPage);

        // when
        List<SummaryResponse> result = elasticsearchSummaryService.recommendSimilarSummaries(summaryId, topK);

        // then
        assertNotNull(result, "폴백 결과가 null입니다.");
        assertEquals(2, result.size(), "폴백 결과 개수가 예상과 다릅니다.");

        // 폴백 메커니즘이 호출되었는지 확인
        verify(summaryRepository, times(1)).findPopularSummariesByPublishStatus(
                eq(PublishStatus.PUBLISHED),
                any(Pageable.class));
    }

    @Test
    @DisplayName("유사한 요약본 추천 테스트 - 검색 실패 시 폴백 메커니즘")
    void recommendSimilarSummariesFallbackTest() throws Exception {
        // given
        Long summaryId = 1L;
        int topK = 2;

        // Mock the repository to return the base summary document
        when(elasticsearchRepository.findById(String.valueOf(summaryId)))
                .thenReturn(Optional.of(baseSummaryDocument));

        // Mock the elasticsearch client to throw an exception
        when(elasticsearchClient.search(any(Function.class), eq(SummaryDocument.class)))
                .thenThrow(new IOException("Elasticsearch 검색 실패"));

        // Mock the fallback mechanism (popular summaries)
        List<Summary> popularSummaries = Arrays.asList(popularSummary1, popularSummary2);
        PageImpl<Summary> popularSummariesPage = new PageImpl<>(popularSummaries);

        when(summaryRepository.findPopularSummariesByPublishStatus(
                eq(PublishStatus.PUBLISHED),
                any(Pageable.class)))
                .thenReturn(popularSummariesPage);

        // when
        List<SummaryResponse> result = elasticsearchSummaryService.recommendSimilarSummaries(summaryId, topK);

        // then
        assertNotNull(result, "폴백 결과가 null입니다.");
        assertEquals(2, result.size(), "폴백 결과 개수가 예상과 다릅니다.");

        // 폴백 결과 확인
        assertEquals(4L, result.get(0).summaryId(), "첫 번째 폴백 결과의 ID가 예상과 다릅니다.");
        assertEquals("인기 요약본 1", result.get(0).title(), "첫 번째 폴백 결과의 제목이 예상과 다릅니다.");

        assertEquals(5L, result.get(1).summaryId(), "두 번째 폴백 결과의 ID가 예상과 다릅니다.");
        assertEquals("인기 요약본 2", result.get(1).title(), "두 번째 폴백 결과의 제목이 예상과 다릅니다.");

        // 폴백 메커니즘이 호출되었는지 확인
        verify(summaryRepository, times(1)).findPopularSummariesByPublishStatus(
                eq(PublishStatus.PUBLISHED),
                any(Pageable.class));
    }

    @Test
    @DisplayName("유사한 요약본 추천 테스트 - 입력값 검증 (음수 topK)")
    void recommendSimilarSummariesNegativeTopKTest() throws Exception {
        // given
        Long summaryId = 1L;
        int topK = -5; // 음수 값
        int expectedTopK = 5; // 기본값으로 조정될 것으로 예상
        String cacheKey = "similar_summaries:" + summaryId + ":" + expectedTopK;

        // Mock the cache to return null (cache miss)
        when(valueOperations.get(cacheKey)).thenReturn(null);

        // Mock the repository to return the base summary document
        when(elasticsearchRepository.findById(String.valueOf(summaryId)))
                .thenReturn(Optional.of(baseSummaryDocument));

        // Mock the search response
        SearchResponse<SummaryDocument> mockResponse = mock(SearchResponse.class);
        HitsMetadata<SummaryDocument> mockHits = mock(HitsMetadata.class);

        // Create mock hits
        Hit<SummaryDocument> hit1 = mock(Hit.class);
        when(hit1.source()).thenReturn(similarSummaryDocument1);

        Hit<SummaryDocument> hit2 = mock(Hit.class);
        when(hit2.source()).thenReturn(similarSummaryDocument2);

        List<Hit<SummaryDocument>> hitsList = Arrays.asList(hit1, hit2);

        // Set up the mock response
        when(mockHits.hits()).thenReturn(hitsList);
        when(mockResponse.hits()).thenReturn(mockHits);

        // Mock the elasticsearch client search method
        when(elasticsearchClient.search(any(Function.class), eq(SummaryDocument.class)))
                .thenReturn(mockResponse);

        // Mock embedding cache
        when(valueOperations.get(eq("embedding:" + summaryId))).thenReturn(null);

        // when
        List<SummaryResponse> result = elasticsearchSummaryService.recommendSimilarSummaries(summaryId, topK);

        // then
        assertNotNull(result, "결과가 null입니다.");
        // 결과 검증 - 실제 결과 개수는 모킹된 응답에 따라 달라질 수 있음

        // Verify that the result was cached
        verify(valueOperations).set(eq(cacheKey), any(List.class), any(Duration.class));
    }

    @Test
    @DisplayName("캐시 무효화 테스트 - 요약 삭제 시")
    void invalidateCacheOnDeleteTest() {
        // given
        Long summaryId = 1L;
        String documentId = String.valueOf(summaryId);

        // Mock Redis keys method
        when(redisTemplate.keys("similar_summaries:" + summaryId + ":*"))
                .thenReturn(new java.util.HashSet<>(Arrays.asList(
                        "similar_summaries:" + summaryId + ":5",
                        "similar_summaries:" + summaryId + ":10"
                )));

        // when
        elasticsearchSummaryService.deleteSummary(summaryId);

        // then
        // Verify that the document was deleted from Elasticsearch
        verify(elasticsearchRepository).deleteById(documentId);

        // Verify that the cache was invalidated
        verify(redisTemplate).delete(any(java.util.Set.class));
        verify(redisTemplate).delete("embedding:" + summaryId);
    }

    @Test
    @DisplayName("캐시 무효화 테스트 - 요약 업데이트 시")
    void invalidateCacheOnUpdateTest() throws Exception {
        // given
        Summary summary = Summary.builder()
                .id(1L)
                .title("업데이트된 요약")
                .brief("업데이트된 요약 내용입니다.")
                .build();

        // Mock embedding client
        List<Float> mockEmbedding = new ArrayList<>();
        for (int i = 0; i < 384; i++) {
            mockEmbedding.add(0.1f + (i * 0.001f));
        }
        when(embeddingClient.embed(anyString(), anyString())).thenReturn(mockEmbedding);

        // Mock the repository to return the base summary document with embedding
        when(elasticsearchRepository.findById(String.valueOf(summary.getId())))
                .thenReturn(Optional.of(baseSummaryDocument));

        // Mock TagService to return tags for the summary
        List<String> summaryTags = Arrays.asList("업데이트", "요약", "테스트");
        when(tagService.getTagNamesBySummary(summary.getId())).thenReturn(summaryTags);

        // Mock Redis keys method for specific summary
        when(redisTemplate.keys("similar_summaries:" + summary.getId() + ":*"))
                .thenReturn(new java.util.HashSet<>(Arrays.asList(
                        "similar_summaries:" + summary.getId() + ":5",
                        "similar_summaries:" + summary.getId() + ":10"
                )));

        // Mock Redis keys method for all summaries (used in fallback)
        when(redisTemplate.keys("similar_summaries:*"))
                .thenReturn(new java.util.HashSet<>(Arrays.asList(
                        "similar_summaries:1:5",
                        "similar_summaries:1:10",
                        "similar_summaries:2:5"
                )));

        // Mock search response for invalidateSimilarSummariesCache
        SearchResponse<SummaryDocument> mockResponse = mock(SearchResponse.class);
        HitsMetadata<SummaryDocument> mockHits = mock(HitsMetadata.class);

        // Create mock hits
        Hit<SummaryDocument> hit1 = mock(Hit.class);
        when(hit1.source()).thenReturn(similarSummaryDocument1);

        Hit<SummaryDocument> hit2 = mock(Hit.class);
        when(hit2.source()).thenReturn(similarSummaryDocument2);

        List<Hit<SummaryDocument>> hitsList = Arrays.asList(hit1, hit2);

        // Set up the mock response
        when(mockHits.hits()).thenReturn(hitsList);
        when(mockResponse.hits()).thenReturn(mockHits);

        // Mock the elasticsearch client search method
        when(elasticsearchClient.search(any(Function.class), eq(SummaryDocument.class)))
                .thenReturn(mockResponse);

        // when
        elasticsearchSummaryService.indexSummary(summary);

        // then
        // Verify that the document was saved to Elasticsearch with tags
        verify(elasticsearchRepository).save(argThat(doc -> {
            if (!(doc instanceof SummaryDocument)) {
                return false;
            }
            SummaryDocument summaryDoc = (SummaryDocument) doc;
            return summaryDoc.getTags() != null && 
                   summaryDoc.getTags().equals(summaryTags) &&
                   summaryDoc.getSummaryId().equals(summary.getId());
        }));

        // Verify that TagService was called to get tags
        verify(tagService).getTagNamesBySummary(summary.getId());

        // Verify that the cache was invalidated
        verify(redisTemplate).delete(any(java.util.Set.class));
        verify(redisTemplate).delete("embedding:" + summary.getId());
    }

    @Test
    @DisplayName("태그 기반 필터링 및 제목 키워드 매칭 테스트")
    void tagBasedFilteringAndTitleKeywordMatchingTest() throws Exception {
        // given
        Long summaryId = 1L;
        int topK = 2;

        // Mock the cache to return null (cache miss)
        when(valueOperations.get(anyString())).thenReturn(null);

        // Mock the repository to return the base summary document
        when(elasticsearchRepository.findById(String.valueOf(summaryId)))
                .thenReturn(Optional.of(baseSummaryDocument));

        // Mock the search response
        SearchResponse<SummaryDocument> mockResponse = mock(SearchResponse.class);
        HitsMetadata<SummaryDocument> mockHits = mock(HitsMetadata.class);

        // Create mock hits - 순서를 의도적으로 바꿔서 제목 키워드 매칭 테스트
        Hit<SummaryDocument> hit1 = mock(Hit.class);
        when(hit1.source()).thenReturn(similarSummaryDocument2); // 머신러닝 (태그 매칭 없음)

        Hit<SummaryDocument> hit2 = mock(Hit.class);
        when(hit2.source()).thenReturn(similarSummaryDocument1); // 인공지능 (태그 매칭 있음)

        List<Hit<SummaryDocument>> hitsList = Arrays.asList(hit1, hit2);

        // Set up the mock response
        when(mockHits.hits()).thenReturn(hitsList);
        when(mockResponse.hits()).thenReturn(mockHits);

        // Mock the elasticsearch client search method
        when(elasticsearchClient.search(any(Function.class), eq(SummaryDocument.class)))
                .thenReturn(mockResponse);

        // when
        List<SummaryResponse> result = elasticsearchSummaryService.recommendSimilarSummaries(summaryId, topK);

        // then
        assertNotNull(result, "결과가 null입니다.");
        assertEquals(2, result.size(), "결과 개수가 예상과 다릅니다.");

        // 제목 키워드 매칭으로 인해 인공지능 관련 문서가 먼저 나와야 함
        assertEquals(2L, result.get(0).summaryId(), "첫 번째 결과의 ID가 예상과 다릅니다.");
        assertEquals("딥러닝과 인공지능", result.get(0).title(), "첫 번째 결과의 제목이 예상과 다릅니다.");

        // 두 번째 결과 확인
        assertEquals(3L, result.get(1).summaryId(), "두 번째 결과의 ID가 예상과 다릅니다.");
        assertEquals("머신러닝 기술 동향", result.get(1).title(), "두 번째 결과의 제목이 예상과 다릅니다.");

        // TagService가 호출되었는지 확인
        verify(tagService, times(1)).getTagNamesBySummary(eq(summaryId));
    }

    @Test
    @DisplayName("선택적 캐시 무효화 테스트 - 유사 요약본 발행 시")
    void invalidateSimilarSummariesCacheTest() throws Exception {
        // given
        Long newSummaryId = 1L;

        // 새 요약본 문서 설정
        when(elasticsearchRepository.findById(String.valueOf(newSummaryId)))
                .thenReturn(Optional.of(baseSummaryDocument));

        // 유사 요약본 검색 결과 설정
        SearchResponse<SummaryDocument> mockResponse = mock(SearchResponse.class);
        HitsMetadata<SummaryDocument> mockHits = mock(HitsMetadata.class);

        // 유사 요약본 목록 생성
        Hit<SummaryDocument> hit1 = mock(Hit.class);
        when(hit1.source()).thenReturn(similarSummaryDocument1);

        Hit<SummaryDocument> hit2 = mock(Hit.class);
        when(hit2.source()).thenReturn(similarSummaryDocument2);

        List<Hit<SummaryDocument>> hitsList = Arrays.asList(hit1, hit2);

        // 검색 응답 설정
        when(mockHits.hits()).thenReturn(hitsList);
        when(mockResponse.hits()).thenReturn(mockHits);

        // Elasticsearch 클라이언트 검색 메서드 모킹
        when(elasticsearchClient.search(any(Function.class), eq(SummaryDocument.class)))
                .thenReturn(mockResponse);

        // 유사 요약본 캐시 키 설정
        when(redisTemplate.keys("similar_summaries:2:*"))
                .thenReturn(new java.util.HashSet<>(Arrays.asList(
                        "similar_summaries:2:5",
                        "similar_summaries:2:10"
                )));

        when(redisTemplate.keys("similar_summaries:3:*"))
                .thenReturn(new java.util.HashSet<>(Arrays.asList(
                        "similar_summaries:3:5"
                )));

        when(redisTemplate.keys("similar_summaries:1:*"))
                .thenReturn(new java.util.HashSet<>(Arrays.asList(
                        "similar_summaries:1:5"
                )));

        // when
        elasticsearchSummaryService.invalidateSimilarSummariesCache(newSummaryId);

        // then
        // 유사 요약본 검색이 수행되었는지 확인
        verify(elasticsearchClient).search(any(Function.class), eq(SummaryDocument.class));

        // 유사 요약본들의 캐시가 무효화되었는지 확인
        verify(redisTemplate).delete(eq(java.util.Set.of("similar_summaries:2:5", "similar_summaries:2:10")));
        verify(redisTemplate).delete(eq(java.util.Set.of("similar_summaries:3:5")));

        // 새 요약본 자신의 캐시도 무효화되었는지 확인
        verify(redisTemplate).delete(eq(java.util.Set.of("similar_summaries:1:5")));
    }
}
