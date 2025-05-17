package joomidang.papersummary.paper.controller;

import static joomidang.papersummary.paper.controller.response.PaperSuccessCode.ANALYSIS_REQUESTED_SUCCESS;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import joomidang.papersummary.auth.resolver.Authenticated;
import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.paper.controller.request.AnalysisRequest;
import joomidang.papersummary.paper.controller.response.AnalysisResponse;
import joomidang.papersummary.paper.service.PaperAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/papers")
@Tag(name = "Paper Analysis", description = "논문 분석 APIs")

public class PaperAnalysisController {

    private final PaperAnalysisService paperAnalysisService;

    //swagger
    @Operation(
            summary = "논문 분석 요청",
            description = "지정된 논문에 대한 분석을 요청합니다. 요청은 비동기적으로 처리되며, 결과는 SSE를 통해 전달됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "분석 요청 성공적으로 접수됨",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AnalysisResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "분석 요청 성공",
                                            summary = "논문 분석 요청 성공 응답",
                                            value = "{\n" +
                                                    "  \"code\": \"PAS-0002\",\n" +
                                                    "  \"message\": \"논문 분석 요청이 성공적으로 처리되었습니다.\",\n" +
                                                    "  \"data\": {\n" +
                                                    "    \"paperId\": 123,\n" +
                                                    "    \"status\": \"파싱 요청 완료\"\n" +
                                                    "  }\n" +
                                                    "}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "논문을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "논문 없음",
                                            summary = "요청한 논문을 찾을 수 없음",
                                            value = "{\n" +
                                                    "  \"code\": \"PAE-0001\",\n" +
                                                    "  \"message\": \"해당 논문을 찾을 수 없습니다. id: 123\",\n" +
                                                    "  \"data\": null\n" +
                                                    "}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류"
            )
    })
    @PostMapping("/{paperId}/analyze")
    public ResponseEntity<ApiResponse<AnalysisResponse>> analyzePaper(
            @Authenticated String providerUid,
            @PathVariable Long paperId,
            @RequestBody(required = false) AnalysisRequest request) {

        log.info("논문 분석 요청 시작: paperId={}, providerUid={}", paperId, providerUid);
        log.debug("분석 요청 컨트롤러 진입: paperId={}, request={}", paperId, request);

        // 요청이 null인 경우 기본값 사용
        String prompt = request != null ? request.prompt() : " ";
        String language = request != null ? request.language() : "ko";
        log.debug("분석 파라미터 설정: prompt='{}', language={}", prompt, language);

        try {
            paperAnalysisService.requestParsing(paperId, providerUid, prompt, language);
            log.info("논문 분석 요청 처리 완료: paperId={}", paperId);

            return ResponseEntity.accepted()
                    .body(ApiResponse.successWithData(
                            ANALYSIS_REQUESTED_SUCCESS,
                            new AnalysisResponse(paperId, "파싱 요청 완료")
                    ));
        } catch (Exception e) {
            log.error("논문 분석 요청 처리 실패: paperId={}, 오류={}", paperId, e.getMessage(), e);
            throw e;
        }
    }
}
