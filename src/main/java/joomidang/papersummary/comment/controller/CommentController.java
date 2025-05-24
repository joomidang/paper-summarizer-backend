package joomidang.papersummary.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import joomidang.papersummary.auth.resolver.Authenticated;
import joomidang.papersummary.comment.controller.request.CommentCreateRequest;
import joomidang.papersummary.comment.controller.request.CommentUpdateRequest;
import joomidang.papersummary.comment.controller.request.ReplyCreateRequest;
import joomidang.papersummary.comment.controller.response.CommentLikeResponse;
import joomidang.papersummary.comment.controller.response.CommentListResponse;
import joomidang.papersummary.comment.controller.response.CommentResponse;
import joomidang.papersummary.comment.controller.response.CommentSuccessCode;
import joomidang.papersummary.comment.entity.Comment;
import joomidang.papersummary.comment.service.CommentService;
import joomidang.papersummary.common.controller.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Comment", description = "댓글 관련 API")
public class CommentController {
    private final CommentService commentService;

    /**
     * 댓글 작성
     */
    @Operation(summary = "댓글 작성", description = "특정 요약본에 댓글을 작성합니다.")
    @PostMapping("/summaries/{summaryId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @Parameter(description = "요약본 ID") @PathVariable Long summaryId,
            @Valid @RequestBody CommentCreateRequest request,
            @Parameter(hidden = true) @Authenticated String providerUid) {

        log.info("댓글 작성 요청: summaryId={}, providerUid={}", summaryId, providerUid);

        CommentResponse response = commentService.createComment(providerUid, summaryId, request.content());

        return ResponseEntity.ok(ApiResponse.successWithData(CommentSuccessCode.COMMENT_CREATED, response));
    }

    /**
     * 대댓글 작성
     */
    @Operation(summary = "대댓글 작성", description = "특정 댓글에 대한 답글을 작성합니다.")
    @PostMapping("/summaries/{summaryId}/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<CommentResponse>> createReply(
            @Parameter(description = "요약본 ID") @PathVariable Long summaryId,
            @Parameter(description = "부모 댓글 ID") @PathVariable Long commentId,
            @Valid @RequestBody ReplyCreateRequest request,
            @Parameter(hidden = true) @Authenticated String providerUid) {

        log.info("대댓글 작성 요청: summaryId={}, parentCommentId={}, providerUid={}",
                summaryId, commentId, providerUid);

        CommentResponse response = commentService.createReply(providerUid, summaryId, commentId, request.content());
        ;

        return ResponseEntity.ok(ApiResponse.successWithData(CommentSuccessCode.REPLY_CREATED, response));
    }

    /**
     * 요약본의 댓글 목록 조회 (페이징)
     */
    @Operation(summary = "댓글 목록 조회", description = "특정 요약본의 댓글 목록을 페이징으로 조회합니다.")
    @GetMapping("/summaries/{summaryId}/comments")
    public ResponseEntity<ApiResponse<CommentListResponse>> getCommentsBySummary(
            @Parameter(description = "요약본 ID") @PathVariable Long summaryId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {

        log.info("댓글 목록 조회 요청: summaryId={}, page={}, size={}",
                summaryId, pageable.getPageNumber(), pageable.getPageSize());

        CommentListResponse response = commentService.getCommentsBySummaryWithPaging(summaryId, pageable);

        return ResponseEntity.ok(ApiResponse.successWithData(CommentSuccessCode.COMMENTS_FETCHED, response));
    }

    /**
     * 댓글 단건 조회
     */
    @Operation(summary = "댓글 조회", description = "특정 댓글의 상세 정보를 조회합니다.")
    @GetMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> getComment(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId) {

        log.info("댓글 단건 조회 요청: commentId={}", commentId);

        Comment comment = commentService.getCommentById(commentId);
        CommentResponse response = CommentResponse.from(comment);

        return ResponseEntity.ok(ApiResponse.successWithData(CommentSuccessCode.COMMENT_FETCHED, response));
    }

    /**
     * 댓글 수정
     */
    @Operation(summary = "댓글 수정", description = "본인이 작성한 댓글을 수정합니다.")
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            @Parameter(hidden = true) @Authenticated String providerUid) {

        log.info("댓글 수정 요청: commentId={}, providerUid={}", commentId, providerUid);

        CommentResponse response = commentService.updateComment(providerUid, commentId, request.content());

        return ResponseEntity.ok(ApiResponse.successWithData(CommentSuccessCode.COMMENT_UPDATED, response));
    }

    /**
     * 댓글 삭제
     */
    @Operation(summary = "댓글 삭제", description = "본인이 작성한 댓글을 삭제합니다.")
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @Parameter(hidden = true) @Authenticated String providerUid) {

        log.info("댓글 삭제 요청: commentId={}, providerUid={}", commentId, providerUid);

        commentService.deleteComment(providerUid, commentId);

        return ResponseEntity.ok(ApiResponse.success(CommentSuccessCode.COMMENT_DELETED));
    }

    /**
     * 댓글 좋아요/취소
     */
    @Operation(
            summary = "댓글 좋아요/좋아요 취소",
            description = "댓글에 좋아요를 추가하거나 제거합니다.",
            parameters = {
                    @Parameter(name = "commentId", description = "댓글 ID", required = true, in = ParameterIn.PATH, example = "1"),
                    @Parameter(name = "action", description = "like 또는 dislike", required = true, in = ParameterIn.QUERY, example = "like")
            }
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공적으로 처리됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<CommentLikeResponse>> likeComment(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @Parameter(description = "좋아요 액션") @RequestParam("action") String action,
            @Parameter(hidden = true) @Authenticated String providerUid)
            throws ExecutionException, InterruptedException {

        log.info("댓글 좋아요 {} 요청: commentId={}, providerUid={}", action, commentId, providerUid);

        try {
            CompletableFuture<CommentLikeResponse> futureResponse =
                    commentService.likeComment(providerUid, commentId, action);

            CommentLikeResponse response = futureResponse.get(5, TimeUnit.SECONDS);

            return ResponseEntity.ok(ApiResponse.successWithData(CommentSuccessCode.COMMENT_LIKED, response));

        } catch (TimeoutException e) {
            log.error("댓글 좋아요 처리 타임아웃: commentId={}, action={}", commentId, action);
            throw new RuntimeException("좋아요 처리 중 타임아웃이 발생했습니다.");
        } catch (Exception e) {
            log.error("댓글 좋아요 처리 실패: commentId={}, action={}, error={}",
                    commentId, action, e.getMessage(), e);
            throw e;
        }
    }
}
