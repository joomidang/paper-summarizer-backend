package joomidang.papersummary.paper.controller;

import static joomidang.papersummary.paper.controller.response.PaperSuccessCode.PARSING_REQUESTED_SUCCESS;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.paper.controller.request.ParsingResultRequest;
import joomidang.papersummary.paper.service.PaperParsingCallbackService;
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
@Tag(name = "Paper", description = "논문 분석 관련 API")
public class PaperParsingCallbackController {
    private final PaperParsingCallbackService paperParsingCallbackService;

    @Operation(
            summary = "논문 파싱 결과 콜백 수신",
            description = "MinerU와 같은 외부 파싱 서비스에서 논문 파싱이 완료되면 이 엔드포인트로 결과를 전송합니다. " +
                    "논문 제목, 마크다운 URL, 시각 자료 URL 등의 정보를 수신하고 처리합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "논문 파싱 결과 데이터",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ParsingResultRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "논문 파싱 결과 예시",
                                            summary = "일반적인 논문 파싱 결과 요청 예시",
                                            value = "{\n" +
                                                    "  \"title\": \"# FLAsHATTENTION: Fast and Memory-Efficient Exact Attention with IO-Awareness.\",\n" +
                                                    "  \"markdownUrl\": \"https://paper-dev-test-magic-pdf-output.s3.amazonaws.com/papers/777/paper_777.md\",\n" +
                                                    "  \"contentListUrl\": \"https://paper-dev-test-magic-pdf-output.s3.amazonaws.com/papers/777/content_list.json\",\n" +
                                                    "  \"figures\": [\n" +
                                                    "    \"https://paper-dev-test-magic-pdf-output.s3.amazonaws.com/papers/777/images/figure_1.jpg\"\n" +
                                                    "  ],\n" +
                                                    "  \"tables\": [https://paper-dev-test-magic-pdf-output.s3.amazonaws.com/papers/777/images/table_1.jpg]\n" +
                                                    "}"
                                    ),
                                    @ExampleObject(
                                            name = "이미지 없는 논문 예시",
                                            summary = "이미지가 없는 간단한 논문 파싱 결과",
                                            value = "{\n" +
                                                    "  \"title\": \"# 자연어처리 연구동향: 최신 변환기 모델 분석\",\n" +
                                                    "  \"markdownUrl\": \"https://paper-dev-test-magic-pdf-output.s3.amazonaws.com/papers/778/paper_778.md\",\n" +
                                                    "  \"contentListUrl\": null,\n" +
                                                    "  \"figures\": [],\n" +
                                                    "  \"tables\": []\n" +
                                                    "}"
                                    )
                            }
                    )
            )

    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "파싱 결과 처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "성공 응답",
                                            value = "{\n" +
                                                    "  \"code\": \"PAS-0001\",\n" +
                                                    "  \"message\": \"논문 파싱 결과가 성공적으로 처리되었습니다.\",\n" +
                                                    "  \"data\": null\n" +
                                                    "}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 형식",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "잘못된 요청",
                                            value = "{\n" +
                                                    "  \"code\": \"PAE-0002\",\n" +
                                                    "  \"message\": \"요청 형식이 올바르지 않습니다.\",\n" +
                                                    "  \"data\": null\n" +
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
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "서버 오류",
                                            value = "{\n" +
                                                    "  \"code\": \"COM-0001\",\n" +
                                                    "  \"message\": \"서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.\",\n" +
                                                    "  \"data\": null\n" +
                                                    "}"
                                    )
                            }
                    )
            )
    })
    @PostMapping("/{paperId}/callback")
    public ResponseEntity<ApiResponse<Void>> receiveParsingCallback(
            @PathVariable Long paperId,
            @RequestBody ParsingResultRequest result
    ) {
        log.info("MinerU 파싱 콜백 수신 -> paper={}, markdownUrl={}", result.title(), result.markdownUrl());
        paperParsingCallbackService.process(paperId, result);
        return ResponseEntity.ok(ApiResponse.success(PARSING_REQUESTED_SUCCESS));
    }
}
