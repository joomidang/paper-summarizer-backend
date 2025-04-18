package joomidang.docs.common;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import joomidang.docs.util.RestDocsSupport;
import joomidang.papersummary.common.controller.HealthCheckController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * HealthCheckController API 문서화 테스트 클래스 RestDocsSupport를 상속받아 구현합니다.
 */
public class HealthCheckDocumentation extends RestDocsSupport {

    @Test
    @DisplayName("헬스 체크 API 문서화 테스트")
    void healthCheck() throws Exception {
        // given
        // when
        ResultActions result = mockMvc.perform(
                get("/api/health")
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andDo(createDocument(
                        responseFields(
                                fieldWithPath("status").type(JsonFieldType.STRING)
                                        .description("서비스 상태 (UP: 정상)"),
                                fieldWithPath("timestamp").type(JsonFieldType.STRING)
                                        .description("현재 시간")
                        )
                ));
    }

    @Override
    protected Object initController() {
        return new HealthCheckController();
    }
}