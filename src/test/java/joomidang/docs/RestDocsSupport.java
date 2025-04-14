package joomidang.docs;

import com.fasterxml.jackson.databind.ObjectMapper;
import joomidang.papersummary.PaperSummaryApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;

@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@SpringBootTest(classes = PaperSummaryApplication.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Import(RestDocsConfiguration.class)
public abstract class RestDocsSupport {

    protected MockMvc mockMvc;

    protected ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(MockMvcRestDocumentation.documentationConfiguration(restDocumentation))
                .alwaysDo(MockMvcResultHandlers.print())
                .addFilter(new CharacterEncodingFilter("UTF-8", true))
                .build();
    }

    protected RestDocumentationResultHandler document(String identifier, ResponseFieldsSnippet responseFieldsSnippet) {
        return MockMvcRestDocumentation.document(identifier,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                responseFieldsSnippet);
    }
}