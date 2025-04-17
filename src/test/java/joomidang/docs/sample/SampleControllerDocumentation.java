package joomidang.docs.sample;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import joomidang.docs.util.RestDocsSupport;
import joomidang.papersummary.sample.controller.SampleController;
import joomidang.papersummary.sample.dto.SampleRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * SampleController API 문서화 테스트 클래스 RestDocsSupport를 상속받아 구현하여 API 문서화를 수행합니다.
 */
public class SampleControllerDocumentation extends RestDocsSupport {

    @Test
    @DisplayName("샘플 목록 조회 API 문서화")
    void getAllSamples() throws Exception {
        // given
        // Create a sample first to have data to retrieve
        createSample();

        // when
        ResultActions result = mockMvc.perform(
                get("/api/samples")
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andDo(createDocument(
                        responseFields(
                                fieldWithPath("[].id").type(JsonFieldType.NUMBER)
                                        .description("샘플 ID"),
                                fieldWithPath("[].name").type(JsonFieldType.STRING)
                                        .description("샘플 이름"),
                                fieldWithPath("[].description").type(JsonFieldType.STRING)
                                        .description("샘플 설명"),
                                fieldWithPath("[].category").type(JsonFieldType.STRING)
                                        .description("샘플 카테고리")
                        )
                ));
    }

    @Test
    @DisplayName("샘플 단일 조회 API 문서화")
    void getSampleById() throws Exception {
        // given
        // Create a sample first to have data to retrieve
        createSample();

        // when
        ResultActions result = mockMvc.perform(
                get("/api/samples/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andDo(createDocument(
                        pathParameters(
                                parameterWithName("id").description("샘플 ID")
                        ),
                        responseFields(
                                fieldWithPath("id").type(JsonFieldType.NUMBER)
                                        .description("샘플 ID"),
                                fieldWithPath("name").type(JsonFieldType.STRING)
                                        .description("샘플 이름"),
                                fieldWithPath("description").type(JsonFieldType.STRING)
                                        .description("샘플 설명"),
                                fieldWithPath("category").type(JsonFieldType.STRING)
                                        .description("샘플 카테고리")
                        )
                ));
    }

    @Test
    @DisplayName("샘플 생성 API 문서화")
    void createSample() throws Exception {
        // given
        SampleRequest request = new SampleRequest("Test Sample", "This is a test sample", "TEST");
        String requestBody = convertToJson(request);

        // when
        ResultActions result = mockMvc.perform(
                post("/api/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        result.andExpect(status().isOk())
                .andDo(createDocument(
                        requestFields(
                                fieldWithPath("name").type(JsonFieldType.STRING)
                                        .description("샘플 이름"),
                                fieldWithPath("description").type(JsonFieldType.STRING)
                                        .description("샘플 설명"),
                                fieldWithPath("category").type(JsonFieldType.STRING)
                                        .description("샘플 카테고리")
                        ),
                        responseFields(
                                fieldWithPath("id").type(JsonFieldType.NUMBER)
                                        .description("샘플 ID"),
                                fieldWithPath("name").type(JsonFieldType.STRING)
                                        .description("샘플 이름"),
                                fieldWithPath("description").type(JsonFieldType.STRING)
                                        .description("샘플 설명"),
                                fieldWithPath("category").type(JsonFieldType.STRING)
                                        .description("샘플 카테고리")
                        )
                ));
    }

    @Test
    @DisplayName("샘플 수정 API 문서화")
    void updateSample() throws Exception {
        // given
        // Create a sample first to have data to update
        createSample();

        SampleRequest updateRequest = new SampleRequest("Updated Sample", "This is an updated sample", "UPDATED");
        String requestBody = convertToJson(updateRequest);

        // when
        ResultActions result = mockMvc.perform(
                put("/api/samples/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        result.andExpect(status().isOk())
                .andDo(createDocument(
                        pathParameters(
                                parameterWithName("id").description("샘플 ID")
                        ),
                        requestFields(
                                fieldWithPath("name").type(JsonFieldType.STRING)
                                        .description("샘플 이름"),
                                fieldWithPath("description").type(JsonFieldType.STRING)
                                        .description("샘플 설명"),
                                fieldWithPath("category").type(JsonFieldType.STRING)
                                        .description("샘플 카테고리")
                        ),
                        responseFields(
                                fieldWithPath("id").type(JsonFieldType.NUMBER)
                                        .description("샘플 ID"),
                                fieldWithPath("name").type(JsonFieldType.STRING)
                                        .description("샘플 이름"),
                                fieldWithPath("description").type(JsonFieldType.STRING)
                                        .description("샘플 설명"),
                                fieldWithPath("category").type(JsonFieldType.STRING)
                                        .description("샘플 카테고리")
                        )
                ));
    }

    @Test
    @DisplayName("샘플 삭제 API 문서화")
    void deleteSample() throws Exception {
        // given
        // Create a sample first to have data to delete
        createSample();

        // when
        ResultActions result = mockMvc.perform(
                delete("/api/samples/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isNoContent())
                .andDo(createDocument(
                        pathParameters(
                                parameterWithName("id").description("샘플 ID")
                        )
                ));
    }

    @Override
    protected Object initController() {
        return new SampleController();
    }
}