package joomidang.docs.paper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import joomidang.docs.util.RestDocsSupport;
import joomidang.papersummary.paper.controller.PaperAnalysisController;
import joomidang.papersummary.paper.controller.request.AnalysisRequest;
import joomidang.papersummary.paper.service.PaperAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * PaperAnalysisController API 문서화 테스트 클래스
 * RestDocsSupport를 상속받아 구현하여 논문 분석 API 문서화를 수행합니다.
 */
public class PaperAnalysisControllerDocumentation extends RestDocsSupport {

    private final PaperAnalysisService paperAnalysisService = Mockito.mock(PaperAnalysisService.class);

    @Test
    @DisplayName("논문 분석 요청 API 문서화")
    void analyzePaper() throws Exception {
        // given
        AnalysisRequest request = new AnalysisRequest("요약을 부탁합니다", "ko");
        String requestBody = convertToJson(request);

        doNothing().when(paperAnalysisService).requestParsing(
                anyLong(), anyString(), anyString(), anyString());

        // when
        ResultActions result = mockMvc.perform(
                post("/api/papers/{paperId}/analyze", 1L)
                        .header("X-AUTH-ID", "test-provider-uid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        result.andExpect(status().isAccepted())
                .andDo(createDocument(
                        pathParameters(
                                parameterWithName("paperId").description("논문 ID")
                        ),
                        requestHeaders(
                                headerWithName("X-AUTH-ID").description("사용자 인증 ID")
                        ),
                        requestFields(
                                fieldWithPath("prompt").type(JsonFieldType.STRING)
                                        .description("분석 요청 프롬프트 (선택 사항)"),
                                fieldWithPath("language").type(JsonFieldType.STRING)
                                        .description("응답 언어 (ko: 한국어, en: 영어, 기본값: ko)")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.STRING)
                                        .description("응답 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING)
                                        .description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT)
                                        .description("분석 요청 정보"),
                                fieldWithPath("data.paperId").type(JsonFieldType.NUMBER)
                                        .description("논문 ID"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING)
                                        .description("분석 요청 상태")
                        )
                ));
    }

    @Override
    protected Object initController() {
        return new PaperAnalysisController(paperAnalysisService);
    }
}