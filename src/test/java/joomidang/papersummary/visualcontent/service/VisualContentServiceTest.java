package joomidang.papersummary.visualcontent.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.visualcontent.entity.VisualContent;
import joomidang.papersummary.visualcontent.repository.VisualContentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class VisualContentServiceTest {

    private VisualContentService visualContentService;
    private VisualContentRepository visualContentRepository;

    @BeforeEach
    void setUp() {
        visualContentRepository = mock(VisualContentRepository.class);
        visualContentService = new VisualContentService(visualContentRepository);
    }

    @Test
    @DisplayName("connectToSummary 메소드가 시각 콘텐츠를 요약본에 연결하는지 테스트")
    void connectToSummaryTest() {
        // given
        Long paperId = 1L;
        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getPaperId()).thenReturn(paperId);

        VisualContent visualContent1 = mock(VisualContent.class);
        VisualContent visualContent2 = mock(VisualContent.class);
        List<VisualContent> visualContents = Arrays.asList(visualContent1, visualContent2);

        when(visualContentRepository.findByPaperIdAndSummaryIsNull(paperId)).thenReturn(visualContents);

        // when
        visualContentService.connectToSummary(mockSummary);

        // then
        verify(mockSummary, times(1)).getPaperId();
        verify(visualContentRepository, times(1)).findByPaperIdAndSummaryIsNull(paperId);

        verify(visualContent1, times(1)).connectToSummary(mockSummary);
        verify(visualContent2, times(1)).connectToSummary(mockSummary);

        verify(visualContentRepository, times(1)).saveAll(visualContents);
    }
}