package joomidang.papersummary.paper.controller;

import static joomidang.papersummary.paper.controller.response.PaperSuccessCode.PARSING_REQUESTED_SUCCESS;

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
public class PaperParsingCallbackController {
    private final PaperParsingCallbackService paperParsingCallbackService;

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
