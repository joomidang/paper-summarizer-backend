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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import joomidang.papersummary.auth.resolver.Authenticated;
import joomidang.papersummary.common.config.elasticsearch.service.ElasticsearchSummaryService;
import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.summary.controller.request.SummaryEditRequest;
import joomidang.papersummary.summary.controller.response.SummaryDetailResponse;
import joomidang.papersummary.summary.controller.response.SummaryEditDetailResponse;
import joomidang.papersummary.summary.controller.response.SummaryEditResponse;
import joomidang.papersummary.summary.controller.response.SummaryLikeResponse;
import joomidang.papersummary.summary.controller.response.SummaryListResponse;
import joomidang.papersummary.summary.controller.response.SummaryPublishResponse;
import joomidang.papersummary.summary.controller.response.SummaryResponse;
import joomidang.papersummary.summary.controller.response.SummarySuccessCode;
import joomidang.papersummary.summary.service.SummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
@Validated
@Tag(name = "Summary", description = "논문 요약본 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class SummaryController {

    private final SummaryService summaryService;
    private final ElasticsearchSummaryService elasticsearchSummaryService;

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
    public ResponseEntity<ApiResponse<SummaryListResponse>> getPopularSummaries(
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
        SummaryListResponse response = summaryService.getPopularSummaries(pageable);

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

    @Operation(
            summary = "요약본 검색",
            description = "제목을 기준으로 요약본을 검색합니다. 인기도 순으로 정렬되어 반환됩니다. " +
                    "검색어는 최소 2자 이상이어야 하며, 대소문자를 구분하지 않습니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "검색 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SummaryListResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 검색어 (너무 짧거나 비어있음)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "검색어 길이 부족",
                                            value = "{\n" +
                                                    "  \"code\": \"SUE-0001\",\n" +
                                                    "  \"message\": \"검색어는 최소 2자 이상이어야 합니다.\",\n" +
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
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<SummaryListResponse>> searchSummaries(
            @Parameter(
                    description = "검색어 (최소 2자 이상)",
                    required = true,
                    example = "딥러닝"
            )
            @RequestParam("keyword") @Size(min = 2, message = "검색어는 최소 2자 이상이어야 합니다.") String keyword,

            @Parameter(
                    description = "페이지 번호 (0부터 시작)",
                    example = "0"
            )
            @RequestParam(required = false, defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") int page,

            @Parameter(
                    description = "페이지 크기 (최대 1000개)",
                    example = "20"
            )
            @RequestParam(required = false, defaultValue = "20") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.") int size
    ) {
        log.info("요약본 검색 요청: searchTerm={}, page={}, size={}", keyword, page, size);

        // 페이지 크기 제한
        if (size > 1000) {
            size = 1000;
        }

        Pageable pageable = PageRequest.of(page, size);
        //SummaryListResponse response = summaryService.searchSummaries(keyword, pageable);
        SummaryListResponse response = elasticsearchSummaryService.searchSummaries(keyword, pageable);

        log.info("요약본 검색 완료: searchTerm={}, 검색된 요약본 수={}",
                keyword, response.summaries().size());

        return ResponseEntity.ok(ApiResponse.successWithData(SummarySuccessCode.SUMMARY_FETCHED, response));
    }

    @Operation(summary = "유사 요약 추천", description = "특정 요약을 기준으로 유사한 다른 요약들을 추천합니다.")
    @GetMapping("/{summaryId}/recommendations")
    public ResponseEntity<ApiResponse<List<SummaryResponse>>> recommendSimilarSummaries(
            @Parameter(description = "기준이 될 요약 ID", example = "42")
            @PathVariable Long summaryId,

            @Parameter(description = "추천 개수", example = "5")
            @RequestParam(defaultValue = "5") int size
    ) {
        List<SummaryResponse> recommendations = elasticsearchSummaryService.recommendSimilarSummaries(summaryId, size);
        return ResponseEntity.ok(ApiResponse.successWithData(SummarySuccessCode.SUMMARY_FETCHED, recommendations));
    }

    @Operation(
            summary = "태그별 요약본 목록 조회",
            description = "특정 태그를 가진 요약본 목록을 조회합니다. 페이지네이션과 정렬을 지원합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "태그별 요약본 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류"
            )
    })
    @GetMapping("/tag")
    public ResponseEntity<ApiResponse<SummaryListResponse>> getSummariesByTag(
            @Parameter(description = "태그명", required = true, example = "딥러닝")
            @RequestParam(required = true) String tag,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(required = false, defaultValue = "0") int page,

            @Parameter(description = "페이지 크기 (기본값: 20)", example = "20")
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        log.info("태그별 요약본 목록 조회 요청: tag={}, page={}, size={}", tag, page, size);

        // 페이지 크기 제한
        if (size > 100) {
            size = 100;
        }

        Pageable pageable = PageRequest.of(page, size);
        SummaryListResponse response = summaryService.getSummariesByTag(tag, pageable);

        log.info("태그별 요약본 목록 조회 완료: tag={}, 조회된 요약본 수={}", tag, response.summaries().size());

        return ResponseEntity.ok(ApiResponse.successWithData(SummarySuccessCode.SUMMARY_FETCHED, response));
    }
}