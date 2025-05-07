package joomidang.papersummary.summary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.entity.SummaryVersion;
import joomidang.papersummary.summary.entity.VersionType;
import joomidang.papersummary.summary.repository.SummaryVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class SummaryVersionServiceTest {

    private SummaryVersionService summaryVersionService;
    private SummaryVersionRepository summaryVersionRepository;

    @BeforeEach
    void setUp() {
        summaryVersionRepository = mock(SummaryVersionRepository.class);
        summaryVersionService = new SummaryVersionService(summaryVersionRepository);
    }

    @Test
    @DisplayName("드래프트 버전 생성 테스트")
    void createDraftVersionTest() {
        // given
        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(1L);

        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);

        String s3Key = "test-s3-key.md";
        String title = "Test Title";

        when(summaryVersionRepository.findMaxRevision(1L)).thenReturn(Optional.of(2));

        // when
        summaryVersionService.createDraftVersion(mockSummary, s3Key, title, mockMember);

        // then
        verify(summaryVersionRepository, times(1)).findMaxRevision(1L);
        verify(summaryVersionRepository, times(1)).save(any(SummaryVersion.class));

        // Verify SummaryVersion entity creation
        ArgumentCaptor<SummaryVersion> versionCaptor = ArgumentCaptor.forClass(SummaryVersion.class);
        verify(summaryVersionRepository).save(versionCaptor.capture());
        SummaryVersion capturedVersion = versionCaptor.getValue();

        // Only verify the s3KeyMd field which has a getter
        assertEquals(s3Key, capturedVersion.getS3KeyMd());
    }

    @Test
    @DisplayName("첫 드래프트 버전 생성 테스트 (이전 버전 없음)")
    void createFirstDraftVersionTest() {
        // given
        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(1L);

        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);

        String s3Key = "test-s3-key.md";
        String title = "Test Title";

        when(summaryVersionRepository.findMaxRevision(1L)).thenReturn(Optional.empty());

        // when
        summaryVersionService.createDraftVersion(mockSummary, s3Key, title, mockMember);

        // then
        verify(summaryVersionRepository, times(1)).findMaxRevision(1L);
        verify(summaryVersionRepository, times(1)).save(any(SummaryVersion.class));

        // Verify SummaryVersion entity creation
        ArgumentCaptor<SummaryVersion> versionCaptor = ArgumentCaptor.forClass(SummaryVersion.class);
        verify(summaryVersionRepository).save(versionCaptor.capture());
        SummaryVersion capturedVersion = versionCaptor.getValue();

        // Only verify the s3KeyMd field which has a getter
        assertEquals(s3Key, capturedVersion.getS3KeyMd());
    }

    @Test
    @DisplayName("발행 버전 생성 테스트")
    void createPublishedVersionTest() {
        // given
        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(1L);

        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1L);

        String s3Key = "test-s3-key.md";
        String title = "Test Title";

        when(summaryVersionRepository.findMaxRevision(1L)).thenReturn(Optional.of(3));

        // when
        summaryVersionService.createPublishedVersion(mockSummary, s3Key, title, mockMember);

        // then
        verify(summaryVersionRepository, times(1)).findMaxRevision(1L);
        verify(summaryVersionRepository, times(1)).save(any(SummaryVersion.class));

        // Verify SummaryVersion entity creation
        ArgumentCaptor<SummaryVersion> versionCaptor = ArgumentCaptor.forClass(SummaryVersion.class);
        verify(summaryVersionRepository).save(versionCaptor.capture());
        SummaryVersion capturedVersion = versionCaptor.getValue();

        // Only verify the s3KeyMd field which has a getter
        assertEquals(s3Key, capturedVersion.getS3KeyMd());
    }

    @Test
    @DisplayName("최신 드래프트 버전 조회 테스트")
    void findLatestDraftTest() {
        // given
        Long summaryId = 1L;
        SummaryVersion mockVersion = mock(SummaryVersion.class);

        when(summaryVersionRepository.findTopBySummaryIdAndVersionTypeOrderByCreatedAtDesc(
                summaryId, VersionType.DRAFT)).thenReturn(Optional.of(mockVersion));

        // when
        Optional<SummaryVersion> result = summaryVersionService.findLatestDraft(summaryId);

        // then
        assertTrue(result.isPresent());
        assertEquals(mockVersion, result.get());
        verify(summaryVersionRepository, times(1))
                .findTopBySummaryIdAndVersionTypeOrderByCreatedAtDesc(summaryId, VersionType.DRAFT);
    }

    @Test
    @DisplayName("드래프트 버전이 없을 때 조회 테스트")
    void findLatestDraftWhenNoneExistsTest() {
        // given
        Long summaryId = 1L;

        when(summaryVersionRepository.findTopBySummaryIdAndVersionTypeOrderByCreatedAtDesc(
                summaryId, VersionType.DRAFT)).thenReturn(Optional.empty());

        // when
        Optional<SummaryVersion> result = summaryVersionService.findLatestDraft(summaryId);

        // then
        assertTrue(result.isEmpty());
        verify(summaryVersionRepository, times(1))
                .findTopBySummaryIdAndVersionTypeOrderByCreatedAtDesc(summaryId, VersionType.DRAFT);
    }
}
