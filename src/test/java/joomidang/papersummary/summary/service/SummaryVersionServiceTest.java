package joomidang.papersummary.summary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.s3.service.S3Service;
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
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        summaryVersionRepository = mock(SummaryVersionRepository.class);
        s3Service = mock(S3Service.class);
        summaryVersionService = new SummaryVersionService(summaryVersionRepository, s3Service);
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

    @Test
    @DisplayName("요약본의 모든 버전 삭제 테스트")
    void deleteAllVersionBySummaryTest() {
        // given
        Summary mockSummary = mock(Summary.class);
        when(mockSummary.getId()).thenReturn(1L);

        // Create mock draft versions
        SummaryVersion draftVersion1 = mock(SummaryVersion.class);
        when(draftVersion1.getId()).thenReturn(1L);
        when(draftVersion1.getS3KeyMd()).thenReturn("draft-s3-key-1.md");
        when(draftVersion1.getVersionType()).thenReturn(VersionType.DRAFT);

        SummaryVersion draftVersion2 = mock(SummaryVersion.class);
        when(draftVersion2.getId()).thenReturn(2L);
        when(draftVersion2.getS3KeyMd()).thenReturn("draft-s3-key-2.md");
        when(draftVersion2.getVersionType()).thenReturn(VersionType.DRAFT);

        List<SummaryVersion> draftVersions = Arrays.asList(draftVersion1, draftVersion2);

        // Create mock published versions
        SummaryVersion publishedVersion = mock(SummaryVersion.class);
        when(publishedVersion.getId()).thenReturn(3L);
        when(publishedVersion.getS3KeyMd()).thenReturn("published-s3-key.md");
        when(publishedVersion.getVersionType()).thenReturn(VersionType.PUBLISHED);

        List<SummaryVersion> publishedVersions = Arrays.asList(publishedVersion);

        // Mock repository responses
        when(summaryVersionRepository.findBySummaryAndVersionTypeOrderByCreatedAtDesc(
                mockSummary, VersionType.DRAFT)).thenReturn(draftVersions);
        when(summaryVersionRepository.findBySummaryAndVersionTypeOrderByCreatedAtDesc(
                mockSummary, VersionType.PUBLISHED)).thenReturn(publishedVersions);

        // Mock S3 service
        doNothing().when(s3Service).deleteFile(anyString());

        // when
        summaryVersionService.deleteAllVersionBySummary(mockSummary);

        // then
        // Verify repository calls
        verify(summaryVersionRepository, times(1))
                .findBySummaryAndVersionTypeOrderByCreatedAtDesc(mockSummary, VersionType.DRAFT);
        verify(summaryVersionRepository, times(1))
                .findBySummaryAndVersionTypeOrderByCreatedAtDesc(mockSummary, VersionType.PUBLISHED);

        // Verify S3 file deletions
        verify(s3Service, times(1)).deleteFile("draft-s3-key-1.md");
        verify(s3Service, times(1)).deleteFile("draft-s3-key-2.md");
        verify(s3Service, times(1)).deleteFile("published-s3-key.md");

        // Verify version deletions
        verify(summaryVersionRepository, times(1)).delete(draftVersion1);
        verify(summaryVersionRepository, times(1)).delete(draftVersion2);
        verify(summaryVersionRepository, times(1)).delete(publishedVersion);
    }
}
