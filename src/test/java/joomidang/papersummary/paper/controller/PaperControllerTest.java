package joomidang.papersummary.paper.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import joomidang.papersummary.auth.resolver.Authenticated;
import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.paper.entity.Status;
import joomidang.papersummary.paper.exception.InvalidFileTypeException;
import joomidang.papersummary.paper.service.PaperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class PaperControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private PaperService paperService;

    /**
     * 테스트용 인증 어노테이션 리졸버 X-AUTH-ID 헤더에서 사용자 ID를 추출하여 @Authenticated 어노테이션이 붙은 파라미터에 주입
     */
    static class TestAuthenticatedArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(Authenticated.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {
            return webRequest.getHeader("X-AUTH-ID");
        }
    }

    @BeforeEach
    void setUp() {
        // 의존성 초기화
        objectMapper = new ObjectMapper();
        paperService = mock(PaperService.class);

        // 컨트롤러 설정
        PaperController paperController = new PaperController(paperService);
        mockMvc = MockMvcBuilders.standaloneSetup(paperController)
                .setCustomArgumentResolvers(new TestAuthenticatedArgumentResolver())
                .setControllerAdvice(new PaperExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("사용자의 논문 목록 조회 성공 테스트")
    void getListMyPapersSuccess() throws Exception {
        // given
        String providerUid = "test-provider-uid";

        // 논문 객체 두 개 생성
        Paper paper1 = Paper.builder()
                .id(1L)
                .title("테스트 논문 1")
                .filePath("https://example.com/papers/test-paper1.pdf")
                .fileType("application/pdf")
                .fileSize(1000L)
                .status(Status.PUBLISHED)
                .build();

        Paper paper2 = Paper.builder()
                .id(2L)
                .title("테스트 논문 2")
                .filePath("https://example.com/papers/test-paper2.pdf")
                .fileType("application/pdf")
                .fileSize(2000L)
                .status(Status.PENDING)
                .build();

        List<Paper> papers = List.of(paper1, paper2);

        // PaperService Mock 설정
        when(paperService.findByProviderUid(eq(providerUid))).thenReturn(papers);

        // when & then
        mockMvc.perform(get("/api/papers")
                        .header("X-AUTH-ID", providerUid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PAS-0005"))
                .andExpect(jsonPath("$.message").value("논문 정보를 성공적으로 조회했습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("테스트 논문 1"))
                .andExpect(jsonPath("$.data[0].filePath").value("https://example.com/papers/test-paper1.pdf"))
                .andExpect(jsonPath("$.data[0].fileType").value("application/pdf"))
                .andExpect(jsonPath("$.data[0].fileSize").value(1000))
                .andExpect(jsonPath("$.data[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].title").value("테스트 논문 2"))
                .andExpect(jsonPath("$.data[1].filePath").value("https://example.com/papers/test-paper2.pdf"))
                .andExpect(jsonPath("$.data[1].fileType").value("application/pdf"))
                .andExpect(jsonPath("$.data[1].fileSize").value(2000))
                .andExpect(jsonPath("$.data[1].status").value("PENDING"));
    }

    @Test
    @DisplayName("사용자의 논문 목록이 비어있는 경우 테스트")
    void getListMyPapersEmptyList() throws Exception {
        // given
        String providerUid = "test-provider-uid";

        // 빈 목록 반환하도록 설정
        when(paperService.findByProviderUid(eq(providerUid))).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/papers")
                        .header("X-AUTH-ID", providerUid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PAS-0005"))
                .andExpect(jsonPath("$.message").value("논문 정보를 성공적으로 조회했습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("논문 업로드 성공 테스트")
    void uploadPaperSuccess() throws Exception {
        // given
        String providerUid = "test-provider-uid";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-paper.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        Paper savedPaper = Paper.builder()
                .id(1L)
                .title(null)
                .filePath("https://example.com/papers/test-paper.pdf")
                .fileType("application/pdf")
                .fileSize(file.getSize())
                .status(Status.PENDING)
                .build();

        when(paperService.uploadPaper(any(), eq(providerUid))).thenReturn(savedPaper);

        // when & then
        mockMvc.perform(multipart("/api/papers")
                        .file(file)
                        .header("X-AUTH-ID", providerUid)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PAS-0001"))
                .andExpect(jsonPath("$.message").value("논문이 성공적으로 업로드되었습니다."))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.filePath").value("https://example.com/papers/test-paper.pdf"))
                .andExpect(jsonPath("$.data.fileType").value("application/pdf"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("잘못된 파일 형식 업로드 테스트")
    void uploadPaperInvalidFileType() throws Exception {
        // given
        String providerUid = "test-provider-uid";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "DOCX content".getBytes()
        );

        when(paperService.uploadPaper(any(), eq(providerUid))).thenThrow(new InvalidFileTypeException());

        // when & then
        mockMvc.perform(multipart("/api/papers")
                        .file(file)
                        .header("X-AUTH-ID", providerUid)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAE-0002"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 파일 형식입니다. PDF 파일만 업로드 가능합니다."));
    }

    @Test
    @DisplayName("파일 없이 업로드 요청 테스트")
    void uploadPaperNoFile() throws Exception {
        // given
        String providerUid = "test-provider-uid";

        // when & then
        mockMvc.perform(multipart("/api/papers")
                        .header("X-AUTH-ID", providerUid)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}