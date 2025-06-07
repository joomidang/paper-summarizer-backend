package joomidang.papersummary.common.config.elasticsearch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import joomidang.papersummary.common.config.elasticsearch.entiy.SummaryDocument;
import joomidang.papersummary.common.config.elasticsearch.repository.SummaryElasticsearchRepository;
import joomidang.papersummary.common.embedding.HuggingFaceEmbeddingClient;
import joomidang.papersummary.summary.controller.response.SummaryListResponse;
import joomidang.papersummary.summary.controller.response.SummaryResponse;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.repository.SummaryRepository;
import joomidang.papersummary.tag.service.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

public class ElasticsearchSummaryServiceTest {

    private ElasticsearchSummaryService elasticsearchSummaryService;

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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup RedisTemplate mock
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        elasticsearchSummaryService = new ElasticsearchSummaryService(
                elasticsearchRepository,
                summaryRepository,
                embeddingClient,
                elasticsearchClient,
                redisTemplate,
                tagService
        );
    }

    @Test
    @DisplayName("텍스트 검색 성공 테스트")
    void searchSummariesSuccess() {
        // given
        String keyword = "인공지능";
        Pageable pageable = PageRequest.of(0, 10);

        // Create mock documents
        SummaryDocument doc1 = createMockSummaryDocument(1L, "인공지능 논문 요약", "인공지능 관련 논문 요약입니다.");
        SummaryDocument doc2 = createMockSummaryDocument(2L, "딥러닝과 인공지능", "딥러닝과 인공지능에 관한 요약입니다.");

        List<SummaryDocument> documents = Arrays.asList(doc1, doc2);
        Page<SummaryDocument> documentsPage = new PageImpl<>(documents, pageable, 2L);

        // Mock repository response
        when(elasticsearchRepository.findByCombinedTextContainingIgnoreCase(keyword, pageable))
                .thenReturn(documentsPage);

        // when
        SummaryListResponse response = elasticsearchSummaryService.searchSummaries(keyword, pageable);

        // then
        assertNotNull(response);
        assertEquals(2, response.summaries().size());
        assertEquals(0, response.currentPage());
        assertEquals(1, response.totalPages());
        assertEquals(2L, response.totalElements());
        assertFalse(response.hasNext());
        assertFalse(response.hasPrevious());

        // Verify first summary
        SummaryResponse firstSummary = response.summaries().get(0);
        assertEquals(1L, firstSummary.summaryId());
        assertEquals("인공지능 논문 요약", firstSummary.title());
        assertEquals("인공지능 관련 논문 요약입니다.", firstSummary.brief());

        // Verify second summary
        SummaryResponse secondSummary = response.summaries().get(1);
        assertEquals(2L, secondSummary.summaryId());
        assertEquals("딥러닝과 인공지능", secondSummary.title());

        // Verify repository call
        verify(elasticsearchRepository, times(1)).findByCombinedTextContainingIgnoreCase(keyword, pageable);
    }

    @Test
    @DisplayName("검색어가 너무 짧을 때 예외 발생 테스트")
    void searchSummariesTooShortKeyword() {
        // given
        String keyword = "a"; // 2글자 미만의 검색어
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            elasticsearchSummaryService.searchSummaries(keyword, pageable);
        });

        assertEquals("검색어는 최소 2자 이상이어야 합니다.", exception.getMessage());

        // Verify repository not called
        verify(elasticsearchRepository, times(0)).findByCombinedTextContainingIgnoreCase(anyString(),
                any(Pageable.class));
    }

    @Test
    @DisplayName("유사 요약본 추천 성공 테스트")
    void recommendSimilarSummariesSuccess() throws IOException {
        // given
        Long summaryId = 1L;
        int topK = 5;

        SummaryDocument baseDoc = createMockSummaryDocument(summaryId, "인공지능 기초", "인공지능 기초에 관한 요약입니다.");
        baseDoc.setEmbedding(new float[]{0.1f, 0.2f, 0.3f}); // Sample embedding
        baseDoc.setTags(Arrays.asList("AI", "Machine Learning"));

        when(elasticsearchRepository.findById(String.valueOf(summaryId)))
                .thenReturn(Optional.of(baseDoc));

        when(valueOperations.get(anyString())).thenReturn(null);

        SummaryDocument doc1 = createMockSummaryDocument(2L, "머신러닝 입문", "머신러닝 입문 요약입니다.");
        SummaryDocument doc2 = createMockSummaryDocument(3L, "딥러닝 기초", "딥러닝 기초 요약입니다.");

        List<Summary> popularSummaries = new ArrayList<>();
        Summary mockSummary1 = mock(Summary.class);
        when(mockSummary1.getId()).thenReturn(2L);
        when(mockSummary1.getTitle()).thenReturn("머신러닝 입문");
        when(mockSummary1.getBrief()).thenReturn("머신러닝 입문 요약입니다.");
        when(mockSummary1.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(mockSummary1.getUpdatedAt()).thenReturn(LocalDateTime.now());

        Summary mockSummary2 = mock(Summary.class);
        when(mockSummary2.getId()).thenReturn(3L);
        when(mockSummary2.getTitle()).thenReturn("딥러닝 기초");
        when(mockSummary2.getBrief()).thenReturn("딥러닝 기초 요약입니다.");
        when(mockSummary2.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(mockSummary2.getUpdatedAt()).thenReturn(LocalDateTime.now());

        popularSummaries.add(mockSummary1);
        popularSummaries.add(mockSummary2);

        Page<Summary> popularSummariesPage = new PageImpl<>(popularSummaries);
        when(summaryRepository.findPopularSummariesByPublishStatus(eq(PublishStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(popularSummariesPage);
        // when
        List<SummaryResponse> recommendations = elasticsearchSummaryService.recommendSimilarSummaries(summaryId, topK);

        // then
        assertNotNull(recommendations);
        assertEquals(2, recommendations.size());

        // Verify
        assertEquals(2L, recommendations.get(0).summaryId());
        assertEquals("머신러닝 입문", recommendations.get(0).title());

        // Verify
        assertEquals(3L, recommendations.get(1).summaryId());
        assertEquals("딥러닝 기초", recommendations.get(1).title());

        // Verify
        verify(elasticsearchRepository, times(1)).findById(String.valueOf(summaryId));
        verify(valueOperations, times(2)).get(
                anyString()); // Cache check (once for recommendations, once for embedding)
        verify(valueOperations, times(2)).set(anyString(), any(),
                any()); // Cache set (once for embedding, once for recommendations)
        verify(summaryRepository, times(1)).findPopularSummariesByPublishStatus(eq(PublishStatus.PUBLISHED),
                any(Pageable.class));
    }

    @Test
    @DisplayName("요약본 인덱싱 성공 테스트")
    void indexSummarySuccess() {
        // given
        Summary summary = mock(Summary.class);
        when(summary.getId()).thenReturn(1L);
        when(summary.getTitle()).thenReturn("인공지능 논문 요약");
        when(summary.getBrief()).thenReturn("인공지능 관련 논문 요약입니다.");
        when(summary.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(summary.getUpdatedAt()).thenReturn(LocalDateTime.now());
        when(summary.getLikeCount()).thenReturn(10);
        when(summary.getViewCount()).thenReturn(100);

        // Mock tag service
        List<String> tags = Arrays.asList("AI", "Machine Learning");
        when(tagService.getTagNamesBySummary(1L)).thenReturn(tags);

        // Mock embedding client
        List<Float> embeddingResult = Arrays.asList(0.1f, 0.2f, 0.3f);
        when(embeddingClient.embed(anyString(), anyString())).thenReturn(embeddingResult);

        // when
        elasticsearchSummaryService.indexSummary(summary);

        // then
        verify(elasticsearchRepository, times(1)).save(any(SummaryDocument.class));
        verify(tagService, times(1)).getTagNamesBySummary(1L);
        verify(embeddingClient, times(1)).embed(anyString(), anyString());
    }

    @Test
    @DisplayName("요약본 삭제 성공 테스트")
    void deleteSummarySuccess() {
        // given
        Long summaryId = 1L;

        // when
        elasticsearchSummaryService.deleteSummary(summaryId);

        // then
        verify(elasticsearchRepository, times(1)).deleteById(String.valueOf(summaryId));
        verify(redisTemplate, times(1)).keys(anyString());
        verify(redisTemplate, times(1)).delete(anyString());
    }

    @Test
    @DisplayName("캐시 무효화 성공 테스트")
    void invalidateRecommendationCacheSuccess() {
        // given
        Long summaryId = 1L;
        Set<String> cacheKeys = Collections.singleton("similar_summaries:1:5");

        when(redisTemplate.keys(anyString())).thenReturn(cacheKeys);

        // when
        elasticsearchSummaryService.invalidateRecommendationCache(summaryId);

        // then
        verify(redisTemplate, times(1)).keys(anyString());
        verify(redisTemplate, times(1)).delete(cacheKeys);
        verify(redisTemplate, times(1)).delete(anyString()); // For embedding cache
    }

    // Helper method to create mock SummaryDocument
    private SummaryDocument createMockSummaryDocument(Long id, String title, String brief) {
        return SummaryDocument.builder()
                .id(id)
                .summaryId(id)
                .title(title)
                .brief(brief)
                .combinedText(title + " " + brief)
                .likeCount(10)
                .viewCount(100)
                .createdAt(LocalDateTime.now())
                .publishedAt(LocalDateTime.now())
                .build();
    }
}
