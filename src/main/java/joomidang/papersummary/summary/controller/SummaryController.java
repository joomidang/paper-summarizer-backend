package joomidang.papersummary.summary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import joomidang.papersummary.auth.resolver.Authenticated;
import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.summary.controller.request.SummaryEditRequest;
import joomidang.papersummary.summary.controller.response.PopularSummaryListResponse;
import joomidang.papersummary.summary.controller.response.SummaryDetailResponse;
import joomidang.papersummary.summary.controller.response.SummaryEditDetailResponse;
import joomidang.papersummary.summary.controller.response.SummaryEditResponse;
import joomidang.papersummary.summary.controller.response.SummaryLikeResponse;
import joomidang.papersummary.summary.controller.response.SummaryPublishResponse;
import joomidang.papersummary.summary.controller.response.SummarySuccessCode;
import joomidang.papersummary.summary.service.SummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 요약본 관련 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/summaries")
@RequiredArgsConstructor
@Tag(name = "Summary", description = "논문 요약본 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class SummaryController {

    private final SummaryService summaryService;

    /**
     * 요약본 단건 조회
     */
    @Operation(
            summary = "요약본 상세 조회",
            description = "특정 요약본의 상세 정보를 조회합니다. 발행된(PUBLISHED) 요약본만 조회 가능합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요약본 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "요약본을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음")
    })
    @GetMapping("/{summaryId}")
    public ResponseEntity<ApiResponse<SummaryDetailResponse>> getSummaryDetail(
            @Parameter(description = "조회할 요약본 ID", required = true, example = "1")
            @PathVariable Long summaryId) {
        SummaryDetailResponse response = summaryService.getSummaryDetail(summaryId);
        return ResponseEntity.ok(ApiResponse.successWithData(SummarySuccessCode.SUMMARY_FETCHED, response));
    }

    @Operation(
            summary = "인기 요약본 목록 조회",
            description = "좋아요, 댓글, 조회수를 가중치로 계산한 인기도 기준으로 요약본 목록을 조회합니다. " +
                    "가중치: 좋아요 50%, 댓글 30%, 조회수 20%"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "인기 요약본 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류"
            )
    })
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<PopularSummaryListResponse>> getPopularSummaries(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(required = false, defaultValue = "0") int page,

            @Parameter(description = "페이지 크기 (기본값: 20)", example = "20")
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        log.info("인기 요약본 목록 조회 요청: page={}, size={}", page, size);

        // 페이지 크기 제한
        if (size > 100) {
            size = 100;
        }

        Pageable pageable = PageRequest.of(page, size);
        PopularSummaryListResponse response = summaryService.getPopularSummaries(pageable);

        log.info("인기 요약본 목록 조회 완료: 조회된 요약본 수={}", response.summaries().size());

        return ResponseEntity.ok(ApiResponse.successWithData(SummarySuccessCode.SUMMARY_FETCHED, response));
    }

    /**
     * 요약본 편집을 위한 상세 정보 조회 API
     */
    @Operation(
            summary = "요약본 편집 정보 조회",
            description = "요약본 편집을 위한 상세 정보를 조회합니다. 본인이 작성한 요약본만 편집할 수 있습니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요약본 편집 정보 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "요약본을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음")
    })
    @GetMapping("/{summaryId}/edit")
    public ResponseEntity<ApiResponse<SummaryEditDetailResponse>> getSummaryForEdit(
            @Parameter(hidden = true)
            @Authenticated String providerUid,

            @Parameter(description = "편집할 요약본 ID", required = true, example = "1")
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
            @Parameter(hidden = true)
            @Authenticated String providerUid,
            @PathVariable Long summaryId) {
        log.debug("요약본 삭제 요청 : summaryId={}, providerUid={}", summaryId, providerUid);
        summaryService.deleteSummary(providerUid, summaryId);
        return ResponseEntity.ok(ApiResponse.success(SummarySuccessCode.SUMMARY_DELETED));
    }

    /**
     * 요약본 편집 내용 임시저장 API 최대 5개만 s3에 보관하도록
     */
    @Operation(
            summary = "요약본 임시 저장",
            description = "요약본 편집 내용을 임시 저장합니다. 최대 5개 버전만 보관합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요약본 임시 저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "요약본을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음")
    })
    @PostMapping("/{summaryId}/draft")
    public ResponseEntity<ApiResponse<SummaryEditResponse>> saveSummaryEdit(
            @Parameter(hidden = true)
            @Authenticated String providerUid,

            @Parameter(description = "저장할 요약본 ID", required = true, example = "1")
            @PathVariable Long summaryId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "요약본 편집 내용",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SummaryEditRequest.class)
                    )
            )
            @RequestBody SummaryEditRequest request) {
        log.debug("요약본 편집 내용 저장 요청: summaryId={}", summaryId);

        SummaryEditResponse response = summaryService.saveSummaryEdit(providerUid, summaryId, request);

        log.debug("요약본 편집 내용 저장 응답: summaryId={}", summaryId);
        return ResponseEntity.ok(ApiResponse.successWithData(SummarySuccessCode.SUMMARY_UPDATED, response));
    }

    /**
     * 요약본 업로드 변경된 파트만 업데이트, s3에 변경된 md 파일 올리기
     */
    @Operation(
            summary = "요약본 발행",
            description = "편집한 요약본을 발행합니다. 발행된 요약본은 다른 사용자가 볼 수 있습니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요약본 발행 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "요약본을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음")})
    @PostMapping("/{summaryId}/publish")
    public ResponseEntity<ApiResponse<SummaryPublishResponse>> publishSummary(
            @Parameter(hidden = true)
            @Authenticated String providerUid,

            @Parameter(description = "발행할 요약본 ID", required = true, example = "1")
            @PathVariable Long summaryId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "요약본 발행 내용",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SummaryEditRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "요약본 발행 요청",
                                            value = "{\n" +
                                                    "  \"title\": \"딥러닝을 활용한 자연어처리 기법 연구\",\n" +
                                                    "  \"brief\": \"본 논문은 최신 딥러닝 기술을 활용한 자연어처리 방법을 제안합니다.\",\n" +
                                                    "  \"markdownContent\": \"# 딥러닝을 활용한 자연어처리 기법 연구\\n\\n## 초록\\n본 논문은 최신 딥러닝 기술을 활용한 자연어처리 방법을 제안합니다.\\n\\n## 1. 서론\\n최근 자연어 처리 분야에서는 딥러닝 모델이 큰 성과를 보이고 있습니다...\",\n"
                                                    +
                                                    "  \"tags\": [\"딥러닝\", \"자연어처리\", \"NLP\"]\n" +
                                                    "}"
                                    )
                            }
                    )
            )
            @RequestBody SummaryEditRequest request) {
        log.debug("요약본 발행 요청: summaryId={}", summaryId);

        SummaryPublishResponse response = summaryService.publishSummary(providerUid, summaryId, request);
        return ResponseEntity.ok(ApiResponse.successWithData(SummarySuccessCode.SUMMARY_PUBLISH, response));
    }

    @Operation(
            summary = "요약본 좋아요 토글",
            description = "요약본에 좋아요를 추가하거나 제거합니다.",
            parameters = {
                    @Parameter(name = "summaryId", description = "요약본 ID", required = true, in = ParameterIn.PATH, example = "1"),
            },
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공적으로 처리됨",
                            content = @Content(schema = @Schema(implementation = SummaryLikeResponse.class))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "요약본을 찾을 수 없음"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
            }
    )
    @PostMapping("/{summaryId}/like")
    public ResponseEntity<ApiResponse<SummaryLikeResponse>> likeSummary(
            @Parameter(hidden = true)
            @Authenticated String providerUid,
            @PathVariable Long summaryId
    ) {
        SummaryLikeResponse response = summaryService.toggleLikeSummary(providerUid, summaryId);
        return ResponseEntity.ok(ApiResponse.successWithData(SummarySuccessCode.SUMMARY_LIKE, response));
    }
}