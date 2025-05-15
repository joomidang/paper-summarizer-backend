package joomidang.papersummary.summary.controller;

import joomidang.papersummary.auth.resolver.Authenticated;
import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.summary.controller.request.SummaryEditRequest;
import joomidang.papersummary.summary.controller.response.SummaryDetailResponse;
import joomidang.papersummary.summary.controller.response.SummaryEditDetailResponse;
import joomidang.papersummary.summary.controller.response.SummaryEditResponse;
import joomidang.papersummary.summary.controller.response.SummaryPublishResponse;
import joomidang.papersummary.summary.controller.response.SummarySuccessCode;
import joomidang.papersummary.summary.service.SummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 요약본 관련 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/summaries")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    /**
     * 요약본 단건 조회
     */
    @GetMapping("/{summaryId}")
    public ResponseEntity<ApiResponse<SummaryDetailResponse>> getSummaryDetail(@PathVariable Long summaryId) {
        SummaryDetailResponse response = summaryService.getSummaryDetail(summaryId);
        return ResponseEntity.ok(ApiResponse.successWithData(SummarySuccessCode.SUMMARY_FETCHED, response));
    }

    /**
     * 요약본 편집을 위한 상세 정보 조회 API
     */
    @GetMapping("/{summaryId}/edit")
    public ResponseEntity<ApiResponse<SummaryEditDetailResponse>> getSummaryForEdit(
            @Authenticated String providerUid,
            @PathVariable Long summaryId) {
        log.debug("요약본 편집을 위한 상세 정보 조회 요청: summaryId={}, providerUid={}", summaryId, providerUid);

        SummaryEditDetailResponse response = summaryService.getSummaryForEdit(providerUid, summaryId);

        log.debug("요약본 편집을 위한 상세 정보 조회 응답: summaryId={}", summaryId);
        return ResponseEntity.ok(ApiResponse.successWithData(SummarySuccessCode.SUMMARY_FETCHED, response));
    }

    /**
     * 요약본 삭제
     */
    @DeleteMapping("/{summaryId}")
    public ResponseEntity<ApiResponse<Void>> deleteSummary(
            @Authenticated String providerUid,
            @PathVariable Long summaryId) {
        log.debug("요약본 삭제 요청 : summaryId={}, providerUid={}", summaryId, providerUid);
        summaryService.deleteSummary(providerUid, summaryId);
        return ResponseEntity.ok(ApiResponse.success(SummarySuccessCode.SUMMARY_DELETED));
    }

    /**
     * 요약본 편집 내용 임시저장 API 최대 5개만 s3에 보관하도록
     */
    @PostMapping("/{summaryId}/draft")
    public ResponseEntity<ApiResponse<SummaryEditResponse>> saveSummaryEdit(
            @Authenticated String providerUid,
            @PathVariable Long summaryId,
            @RequestBody SummaryEditRequest request) {
        log.debug("요약본 편집 내용 저장 요청: summaryId={}", summaryId);

        SummaryEditResponse response = summaryService.saveSummaryEdit(providerUid, summaryId, request);

        log.debug("요약본 편집 내용 저장 응답: summaryId={}", summaryId);
        return ResponseEntity.ok(ApiResponse.successWithData(SummarySuccessCode.SUMMARY_UPDATED, response));
    }

    /**
     * 요약본 업로드 변경된 파트만 업데이트, s3에 변경된 md 파일 올리기
     */
    @PostMapping("/{summaryId}/publish")
    public ResponseEntity<ApiResponse<SummaryPublishResponse>> publishSummary(
            @Authenticated String providerUid,
            @PathVariable Long summaryId,
            @RequestBody SummaryEditRequest request) {
        log.debug("요약본 발행 요청: summaryId={}", summaryId);

        SummaryPublishResponse response = summaryService.publishSummary(providerUid, summaryId, request);
        return ResponseEntity.ok(ApiResponse.successWithData(SummarySuccessCode.SUMMARY_PUBLISH, response));
    }

}