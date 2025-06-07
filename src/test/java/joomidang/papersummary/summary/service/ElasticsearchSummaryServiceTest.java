package joomidang.papersummary.summary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import joomidang.papersummary.common.config.elasticsearch.entiy.SummaryDocument;
import joomidang.papersummary.common.config.elasticsearch.repository.SummaryElasticsearchRepository;
import joomidang.papersummary.common.config.elasticsearch.service.ElasticsearchSummaryService;
import joomidang.papersummary.common.embedding.EmbeddingClient;
import joomidang.papersummary.summary.controller.response.SummaryListResponse;
import joomidang.papersummary.summary.repository.SummaryRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * ElasticsearchSummaryService 테스트 - 모킹 방식으로 변경 실제 Elasticsearch 인스턴스에 의존하지 않고 모킹을 통해 테스트
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class ElasticsearchSummaryServiceTest {

    @Mock
    private SummaryElasticsearchRepository elasticsearchRepository;

    @Mock
    private SummaryRepository summaryRepository;

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @InjectMocks
    private ElasticsearchSummaryService elasticsearchSummaryService;

    private SummaryDocument document1;
    private SummaryDocument document2;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 생성
        document1 = SummaryDocument.builder()
                .id(1L)
                .summaryId(1L)
                .title("인공지능 논문 요약")
                .brief("인공지능 관련 논문 요약입니다.")
                .combinedText("인공지능 논문 요약 인공지능 관련 논문 요약입니다.")
                .likeCount(20)
                .viewCount(100)
                .createdAt(LocalDateTime.now())
                .build();

        document2 = SummaryDocument.builder()
                .id(2L)
                .summaryId(2L)
                .title("딥러닝과 인공지능")
                .brief("딥러닝과 인공지능에 관한 요약입니다.")
                .combinedText("딥러닝과 인공지능 딥러닝과 인공지능에 관한 요약입니다.")
                .likeCount(15)
                .viewCount(80)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Elasticsearch 요약본 검색 성공 테스트 - 결과 있음")
    void searchSummariesSuccess() throws Exception {
        // given
        String searchTerm = "인공지능";
        Pageable pageable = PageRequest.of(0, 10);

        // Mock the repository search method
        List<SummaryDocument> documents = Arrays.asList(document1, document2);
        Page<SummaryDocument> mockPage = new org.springframework.data.domain.PageImpl<>(documents, pageable,
                documents.size());

        when(elasticsearchRepository.findByCombinedTextContainingIgnoreCase(eq(searchTerm), eq(pageable)))
                .thenReturn(mockPage);

        // when
        SummaryListResponse response = elasticsearchSummaryService.searchSummaries(searchTerm, pageable);

        // then
        assertNotNull(response, "검색 응답이 null입니다.");
        assertEquals(2, response.summaries().size(), "검색 결과 개수가 예상과 다릅니다.");
        assertEquals(document1.getTitle(), response.summaries().get(0).title(), "첫 번째 결과의 제목이 예상과 다릅니다.");
        assertEquals(document2.getTitle(), response.summaries().get(1).title(), "두 번째 결과의 제목이 예상과 다릅니다.");
    }

    @Test
    @DisplayName("Elasticsearch 요약본 검색 테스트 - 결과 없음")
    void searchSummariesNoResults() throws Exception {
        // given
        String searchTerm = "존재하지않는검색어";
        Pageable pageable = PageRequest.of(0, 10);

        // Mock the repository search method with empty results
        List<SummaryDocument> emptyList = Collections.emptyList();
        Page<SummaryDocument> emptyPage = new PageImpl<>(emptyList, pageable, 0);

        when(elasticsearchRepository.findByCombinedTextContainingIgnoreCase(eq(searchTerm), eq(pageable)))
                .thenReturn(emptyPage);

        // when
        SummaryListResponse response = elasticsearchSummaryService.searchSummaries(searchTerm, pageable);

        // then
        assertNotNull(response, "검색 응답이 null입니다.");
        assertEquals(0, response.summaries().size(), "검색 결과가 있어서는 안 됩니다.");
    }

    @Test
    @DisplayName("Elasticsearch 문서 조회 테스트")
    void getSummaryDocumentTest() {
        // given
        Long documentId = 1L;

        // Mock repository to return the document
        when(elasticsearchRepository.findById(String.valueOf(documentId))).thenReturn(Optional.of(document1));

        // when
        Optional<SummaryDocument> result = elasticsearchRepository.findById(String.valueOf(documentId));

        // then
        assertNotNull(result, "조회 결과가 null입니다.");
        assertEquals(true, result.isPresent(), "문서가 존재해야 합니다.");
        assertEquals("인공지능 논문 요약", result.get().getTitle(), "문서 제목이 일치하지 않습니다.");
    }
}
