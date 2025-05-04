package joomidang.docs.paper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import joomidang.docs.util.RestDocsSupport;
import joomidang.papersummary.paper.controller.PaperController;
import joomidang.papersummary.paper.controller.response.PaperResponse;
import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.paper.entity.Status;
import joomidang.papersummary.paper.service.PaperService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * PaperController API 문서화 테스트 클래스
 * RestDocsSupport를 상속받아 구현하여 논문 관련 API 문서화를 수행합니다.
 */
public class PaperControllerDocumentation extends RestDocsSupport {

    private final PaperService paperService = Mockito.mock(PaperService.class);

    @Test
    @DisplayName("논문 업로드 API 문서화")
    void uploadPaper() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-paper.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );

        Paper paper = Paper.builder()
                .id(1L)
                .title("test-paper")
                .filePath("papers/test-paper.pdf")
                .fileType("application/pdf")
                .fileSize(12345L)
                .status(Status.PENDING)
                .build();

        when(paperService.uploadPaper(any(), any())).thenReturn(paper);

        // when
        ResultActions result = mockMvc.perform(
                multipart("/api/papers")
                        .file(file)
                        .header("X-AUTH-ID", "test-provider-uid")
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
        );

        // then
        result.andExpect(status().isOk())
                .andDo(createDocument(
                        requestHeaders(
                                headerWithName("X-AUTH-ID").description("사용자 인증 ID")
                        ),
                        requestParts(
                                partWithName("file").description("업로드할 논문 파일 (PDF)")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.STRING)
                                        .description("응답 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING)
                                        .description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT)
                                        .description("논문 정보"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                        .description("논문 ID"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING)
                                        .description("논문 제목"),
                                fieldWithPath("data.filePath").type(JsonFieldType.STRING)
                                        .description("파일 경로"),
                                fieldWithPath("data.fileType").type(JsonFieldType.STRING)
                                        .description("파일 타입"),
                                fieldWithPath("data.fileSize").type(JsonFieldType.NUMBER)
                                        .description("파일 크기 (바이트)"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING)
                                        .description("논문 상태 (PENDING, PROCESSING, ANALYZED, PUBLISHED, FAILED)")
                        )
                ));
    }

    @Override
    protected Object initController() {
        return new PaperController(paperService);
    }
}
