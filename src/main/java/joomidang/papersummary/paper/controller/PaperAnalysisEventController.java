package joomidang.papersummary.paper.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.common.service.SseService;
import joomidang.papersummary.paper.service.PaperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 논문 분석 이벤트 관련 컨트롤러 SSE(Server-Sent Events)를 사용하여 클라이언트에게 실시간 이벤트를 전송
 */
@Slf4j
@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
@Tag(name = "Paper", description = "논문 분석 관련 API")
public class PaperAnalysisEventController {

    private final SseService sseService;
    private final PaperService paperService;

    /**
     * 논문 분석 이벤트 구독 엔드포인트 클라이언트는 이 엔드포인트에 연결하여 논문 분석 관련 이벤트를 수신할 수 있음
     */
    @Operation(
            summary = "논문 분석 이벤트 구독",
            description = "Server-Sent Events(SSE)를 통해 특정 논문의 분석 진행 상태 이벤트를 실시간으로 수신합니다. " +
                    "연결 후 클라이언트는 파싱 완료, 요약 생성 완료 등의 이벤트를 받을 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "이벤트 스트림 연결 성공",
                    content = @Content(
                            mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "파싱 완료 이벤트",
                                            summary = "논문 파싱 완료 이벤트 예시",
                                            value = "event: parsing_completed\n" +
                                                    "data: {\"paperId\": 123, \"timestamp\": \"2023-05-23T12:34:56.789\"}\n\n"
                                    ),
                                    @ExampleObject(
                                            name = "요약 완료 이벤트",
                                            summary = "논문 요약 완료 이벤트 예시",
                                            value = "event: summary_completed\n" +
                                                    "data: {\"paperId\": 123, \"summaryId\": 456, \"timestamp\": \"2023-05-23T12:45:30.123\"}\n\n"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "논문을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "논문 없음",
                                            summary = "요청한 논문을 찾을 수 없음",
                                            value = "{\n" +
                                                    "  \"code\": \"PAE-0001\",\n" +
                                                    "  \"message\": \"요청한 논문을 찾을 수 없습니다: 123\",\n" +
                                                    "  \"data\": null\n" +
                                                    "}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류"
            )
    })


    @GetMapping(value = "/{paperId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToEvents(@PathVariable Long paperId) {
        log.info("논문 분석 이벤트 구독 요청: paperId={}", paperId);
        paperService.findById(paperId);//해당 논문이 존재하는지 확인용
        return sseService.createConnection(paperId);
    }
}