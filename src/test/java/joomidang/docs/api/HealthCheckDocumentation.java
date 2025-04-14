package joomidang.docs.api;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import joomidang.docs.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

public class HealthCheckDocumentation extends RestDocsSupport {
    @Test
    @DisplayName("Health Check API 문서화")
    void healthCheck() throws Exception {
        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("health-check",
                        responseFields(
                                fieldWithPath("status").type(JsonFieldType.STRING).description("서버 상태"),
                                fieldWithPath("timestamp").type(JsonFieldType.STRING).description("현재 시간")
                        )
                ));
    }
}
