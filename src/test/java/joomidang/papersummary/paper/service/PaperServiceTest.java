package joomidang.papersummary.paper.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import joomidang.papersummary.analysislog.entity.AnalysisLog;
import joomidang.papersummary.analysislog.repository.AnalysisLogRepository;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.service.MemberService;
import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.paper.entity.Status;
import joomidang.papersummary.paper.exception.InvalidFileTypeException;
import joomidang.papersummary.paper.repository.PaperRepository;
import joomidang.papersummary.s3.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class PaperServiceTest {

    private PaperService paperService;
    private S3Service s3Service;
    private MemberService memberService;
    private PaperRepository paperRepository;
    private AnalysisLogRepository analysisLogRepository;

    @BeforeEach
    void setUp() {
        s3Service = mock(S3Service.class);
        memberService = mock(MemberService.class);
        paperRepository = mock(PaperRepository.class);
        analysisLogRepository = mock(AnalysisLogRepository.class);

        paperService = new PaperService(
                s3Service,
                memberService,
                paperRepository,
                analysisLogRepository
        );
    }

    @Test
    @DisplayName("PDF 파일 업로드 성공 테스트")
    void uploadPaperSuccess() {
        // given
        String providerUid = "test-provider-uid";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-paper.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        Member mockMember = mock(Member.class);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        String s3Url = "https://example.com/papers/test-paper.pdf";
        when(s3Service.uploadFile(any(MultipartFile.class), eq("papers"))).thenReturn(s3Url);

        Paper mockPaper = Paper.builder()
                .id(1L)
                .title(null)
                .filePath(s3Url)
                .fileType("application/pdf")
                .fileSize(file.getSize())
                .status(Status.PENDING)
                .member(mockMember)
                .build();

        when(paperRepository.save(any(Paper.class))).thenReturn(mockPaper);

        // when
        Paper result = paperService.uploadPaper(file, providerUid);

        // then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(s3Url, result.getFilePath());
        assertEquals("application/pdf", result.getFileType());
        assertEquals(file.getSize(), result.getFileSize());
        assertEquals(Status.PENDING, result.getStatus());

        // Verify interactions
        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(s3Service, times(1)).uploadFile(any(MultipartFile.class), eq("papers"));
        verify(paperRepository, times(1)).save(any(Paper.class));
        verify(analysisLogRepository, times(1)).save(any(AnalysisLog.class));

        // Verify Paper entity creation
        ArgumentCaptor<Paper> paperCaptor = ArgumentCaptor.forClass(Paper.class);
        verify(paperRepository).save(paperCaptor.capture());
        Paper capturedPaper = paperCaptor.getValue();
        assertEquals(s3Url, capturedPaper.getFilePath());
        assertEquals("application/pdf", capturedPaper.getFileType());
        assertEquals(file.getSize(), capturedPaper.getFileSize());
        assertEquals(Status.PENDING, capturedPaper.getStatus());
        assertEquals(mockMember, capturedPaper.getMember());

        // Verify AnalysisLog entity creation
        ArgumentCaptor<AnalysisLog> logCaptor = ArgumentCaptor.forClass(AnalysisLog.class);
        verify(analysisLogRepository).save(logCaptor.capture());
        AnalysisLog capturedLog = logCaptor.getValue();
        assertEquals(mockPaper, capturedLog.getPaper());
        assertEquals(mockMember, capturedLog.getMember());
    }

    @Test
    @DisplayName("잘못된 파일 형식 업로드 테스트")
    void uploadPaperInvalidFileType() {
        // given
        String providerUid = "test-provider-uid";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "DOCX content".getBytes()
        );

        Member mockMember = mock(Member.class);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        // when & then
        assertThrows(InvalidFileTypeException.class, () -> {
            paperService.uploadPaper(file, providerUid);
        });

        // Verify interactions
        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(s3Service, times(0)).uploadFile(any(MultipartFile.class), anyString());
        verify(paperRepository, times(0)).save(any(Paper.class));
        verify(analysisLogRepository, times(0)).save(any(AnalysisLog.class));
    }

    @Test
    @DisplayName("Null 컨텐츠 타입 업로드 테스트")
    void uploadPaperNullContentType() {
        // given
        String providerUid = "test-provider-uid";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-file.unknown",
                null,
                "Unknown content".getBytes()
        );

        Member mockMember = mock(Member.class);
        when(memberService.findByProviderUid(providerUid)).thenReturn(mockMember);

        // when & then
        assertThrows(InvalidFileTypeException.class, () -> {
            paperService.uploadPaper(file, providerUid);
        });

        // Verify interactions
        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(s3Service, times(0)).uploadFile(any(MultipartFile.class), anyString());
        verify(paperRepository, times(0)).save(any(Paper.class));
        verify(analysisLogRepository, times(0)).save(any(AnalysisLog.class));
    }
}