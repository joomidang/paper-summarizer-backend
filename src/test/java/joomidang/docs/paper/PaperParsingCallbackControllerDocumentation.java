package joomidang.docs.paper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import joomidang.docs.util.RestDocsSupport;
import joomidang.papersummary.paper.controller.PaperParsingCallbackController;
import joomidang.papersummary.paper.controller.request.ParsingResultRequest;
import joomidang.papersummary.paper.service.PaperParsingCallbackService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * PaperParsingCallbackController API 문서화 테스트 클래스 RestDocsSupport를 상속받아 구현하여 논문 파싱 콜백 API 문서화를 수행합니다.
 */
public class PaperParsingCallbackControllerDocumentation extends RestDocsSupport {

    private final PaperParsingCallbackService paperParsingCallbackService = Mockito.mock(
            PaperParsingCallbackService.class);

    @Test
    @DisplayName("논문 파싱 콜백 API 문서화")
    void receiveParsingCallback() throws Exception {
        // given
        ParsingResultRequest request = new ParsingResultRequest(
                "Sample Paper Title",
                "https://example.com/markdown/paper123.md",
                "https://example.com/markdown/paper123_content_list.json",
                Arrays.asList("figure1.jpg", "figure2.jpg"),
                Arrays.asList("table1.jpg", "table2.jpg")
        );
        String requestBody = convertToJson(request);

        doNothing().when(paperParsingCallbackService).process(anyLong(), any(ParsingResultRequest.class));

        // when
        ResultActions result = mockMvc.perform(
                post("/api/papers/{paperId}/callback", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        result.andExpect(status().isOk())
                .andDo(createDocument(
                        pathParameters(
                                parameterWithName("paperId").description("논문 ID")
                        ),
                        requestFields(
                                fieldWithPath("title").type(JsonFieldType.STRING)
                                        .description("논문 제목"),
                                fieldWithPath("markdownUrl").type(JsonFieldType.STRING)
                                        .description("마크다운 파일 URL"),
                                fieldWithPath("contentListUrl").type(JsonFieldType.STRING)
                                        .description("컨텐츠 목록 파일"),
                                fieldWithPath("figures").type(JsonFieldType.ARRAY)
                                        .description("논문 내 그림 파일 목록"),
                                fieldWithPath("tables").type(JsonFieldType.ARRAY)
                                        .description("논문 내 표 파일 목록")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.STRING)
                                        .description("응답 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING)
                                        .description("응답 메시지")
                        )
                ));
    }

    @Override
    protected Object initController() {
        return new PaperParsingCallbackController(paperParsingCallbackService);
    }
}