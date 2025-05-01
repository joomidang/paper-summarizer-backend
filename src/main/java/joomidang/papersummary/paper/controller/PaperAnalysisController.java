package joomidang.papersummary.paper.controller;

import static joomidang.papersummary.paper.controller.response.PaperSuccessCode.ANALYSIS_REQUESTED_SUCCESS;

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
public class PaperAnalysisController {

    private final PaperAnalysisService paperAnalysisService;

    @PostMapping("/{paperId}/analyze")
    public ResponseEntity<ApiResponse<AnalysisResponse>> analyzePaper(
            @Authenticated String providerUid,
            @PathVariable Long paperId,
            @RequestBody(required = false) AnalysisRequest request) {

        log.info("분석 요청 시작 : paperId={}, providerUid={}", paperId, providerUid);

        paperAnalysisService.requestParsing(paperId, providerUid, request.prompt(), request.language());

        return ResponseEntity.accepted()
                .body(ApiResponse.successWithData(
                        ANALYSIS_REQUESTED_SUCCESS,
                        new AnalysisResponse(paperId, "파싱 요청 완료")
                ));
    }
}
