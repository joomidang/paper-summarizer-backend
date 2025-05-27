package joomidang.papersummary.comment.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import joomidang.papersummary.comment.controller.response.CommentLikeResponse;
import joomidang.papersummary.comment.controller.response.CommentListResponse;
import joomidang.papersummary.comment.controller.response.CommentResponse;
import joomidang.papersummary.comment.entity.Comment;
import joomidang.papersummary.comment.entity.CommentLike;
import joomidang.papersummary.comment.exception.CommentAccessDeniedException;
import joomidang.papersummary.comment.exception.CommentNotFoundException;
import joomidang.papersummary.comment.exception.DeletedCommentException;
import joomidang.papersummary.comment.exception.InvalidLikeActionException;
import joomidang.papersummary.comment.exception.InvalidParentCommentException;
import joomidang.papersummary.comment.exception.UnpublishedSummaryCommentException;
import joomidang.papersummary.comment.repository.CommentLikeRepository;
import joomidang.papersummary.comment.repository.CommentRepository;
import joomidang.papersummary.common.config.rabbitmq.StatsEventPublisher;
import joomidang.papersummary.common.config.rabbitmq.StatsType;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.service.MemberService;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.service.SummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final SummaryService summaryService;
    private final MemberService memberService;
    private final CommentLikeRepository commentLikeRepository;
    private final StatsEventPublisher statsEventPublisher;

    /**
     * 댓글 작성
     */
    @Transactional
    public CommentResponse createComment(String providerUid, Long summaryId, String content) {
        log.debug("댓글 작성 시작: summaryId={}, providerUid={}", summaryId, providerUid);
        Summary summary = summaryService.findByIdWithoutStats(summaryId);
        Member member = memberService.findByProviderUid(providerUid);
        Comment comment = Comment.builder()
                .content(content)
                .summary(summary)
                .member(member)
                .build();
        Comment savedComment = commentRepository.save(comment);
        statsEventPublisher.publish(summaryId, StatsType.COMMENT);
        log.debug("댓글 작성 완료 : commentId={}", savedComment.getContent());
        return CommentResponse.from(savedComment);
    }

    /**
     * 대댓글 작성
     */
    @Transactional
    public CommentResponse createReply(String providerUid, Long summaryId, Long parentCommentId, String content) {
        log.debug("대댓글 작성 시작: parentCommentId");
        Summary summary = validateSummaryForComment(summaryId);
        Comment parentComment = findCommentById(parentCommentId);

        validateParentComment(summaryId, parentCommentId, parentComment);

        Member member = memberService.findByProviderUid(providerUid);

        Comment replyComment = Comment.builder()
                .content(content)
                .summary(summary)
                .member(member)
                .parent(parentComment)
                .build();

        Comment savedReplyComment = commentRepository.save(replyComment);
        parentComment.addChild(savedReplyComment);
        statsEventPublisher.publish(summaryId, StatsType.COMMENT);
        log.debug("대댓글 작성 완료 : replyCommentId={}", savedReplyComment.getId());

        return CommentResponse.from(savedReplyComment);
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public CommentResponse updateComment(String providerUid, Long commentId, String content) {
        log.debug("댓글 수정 시작: commentId={}, providerUid={}", commentId, providerUid);
        Comment comment = findCommentById(commentId);

        validateCommentNotDeleted(comment);

        validateCommentOwnership(comment, providerUid);

        comment.updateContent(content);

        return CommentResponse.from(comment);
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deleteComment(String providerUid, Long commentId) {
        log.debug("댓글 삭제 시작: commentId={}, providerUid={}", commentId, providerUid);
        Comment comment = findCommentById(commentId);

        validateCommentNotDeleted(comment);

        validateCommentOwnership(comment, providerUid);

        comment.softDelete();
        statsEventPublisher.publish(comment.getSummary().getId(), StatsType.UNCOMMENT);
        log.info("댓글 삭제 완료: commentId={}", commentId);
    }

    /**
     * 특정 요약본의 댓글 목록을 페이징으로 조회 부모 댓글을 페이징으로 조회한 후, 해당 부모들의 자식 댓글들을 일괄 조회하여 N+1 문제 해결
     */
    public CommentListResponse getCommentsBySummaryWithPaging(Long summaryId, Pageable pageable) {
        log.debug("요약본 댓글 조회 시작: summaryId={}, page={}, size={}", summaryId, pageable.getPageNumber(),
                pageable.getPageSize());
        summaryService.findByIdWithoutStats(summaryId);

        // 1단계: 최상위 댓글들만 페이징 조회
        Page<Comment> rootCommentsPage = commentRepository.findRootCommentsBySummaryIdWithPaging(summaryId, pageable);

        // 2단계: 조회된 부모 댓글들의 ID 수집
        List<Long> parentIds = rootCommentsPage.getContent().stream()
                .map(Comment::getId)
                .collect(Collectors.toList());

        // 3단계: 부모 댓글들의 자식 댓글들을 일괄 조회
        if (!parentIds.isEmpty()) {
            List<Comment> children = commentRepository.findChildrenByParentIds(parentIds);

            // 4단계: 부모 댓글에 자식 댓글들 매핑
            Map<Long, List<Comment>> childrenMap = children.stream()
                    .collect(Collectors.groupingBy(child -> child.getParent().getId()));

            // 5단계: 각 부모 댓글에 해당하는 자식 댓글들 설정
            rootCommentsPage.getContent().forEach(parent -> {
                List<Comment> parentChildren = childrenMap.getOrDefault(parent.getId(), Collections.emptyList());
                parent.setChildrenList(parentChildren);
            });
        }

        log.info("요약본 댓글 페이징 조회 완료: summaryId={}, 조회된 댓글 수={}, 전체 페이지={}",
                summaryId, rootCommentsPage.getNumberOfElements(), rootCommentsPage.getTotalPages());

        return CommentListResponse.from(rootCommentsPage);
    }

    /**
     * 댓글 단건 조회
     */
    public Comment getCommentById(Long commentId) {
        log.info("댓글 단건 조회: commentId={}", commentId);

        Comment comment = findCommentById(commentId);

        validateCommentNotDeleted(comment);

        return comment;
    }

    /**
     * 특정 사용자가 작성한 댓글 목록을 페이징으로 조회
     */
    public CommentListResponse getCommentsByMember(String providerUid, Pageable pageable) {
        log.debug("사용자 댓글 조회 시작: providerUid={}, page={}, size={}",
                providerUid, pageable.getPageNumber(), pageable.getPageSize());

        // 사용자 정보 조회
        Member member = memberService.findByProviderUid(providerUid);

        // 사용자가 작성한 삭제되지 않은 댓글들을 페이징 조회
        Page<Comment> commentsPage = commentRepository.findByMemberAndDeletedFalseWithSummary(member, pageable);

        log.info("사용자 댓글 조회 완료: providerUid={}, 조회된 댓글 수={}, 전체 페이지={}",
                providerUid, commentsPage.getNumberOfElements(), commentsPage.getTotalPages());

        return CommentListResponse.from(commentsPage);
    }

    /**
     * 댓글 좋아요/취소(비동기)
     */
    @Async
    @Transactional
    public CompletableFuture<CommentLikeResponse> likeComment(String providerUid, Long commentId, String action) {
        log.debug("댓글 좋아요 {} 처리 시작: commentId={}, providerUid={}", action, commentId, providerUid);
        Member member = memberService.findByProviderUid(providerUid);

        Comment comment = findCommentById(commentId);

        validateCommentNotDeleted(comment);
        validateAction(action);

        CommentLikeResponse response = processLike(member, comment, action);
        log.debug("댓글 좋아요 {} 처리 완료: commentId={}, liked={}, providerUid={}", action, commentId, response.liked(),
                providerUid);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * 댓글 ID로 댓글 조회 (내부 사용)
     */
    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.error("댓글을 찾을 수 없음: commentId={}", commentId);
                    return new CommentNotFoundException(commentId);
                });
    }

    /**
     * 요약본이 댓글 작성 가능한 상태인지 확인
     */
    private Summary validateSummaryForComment(Long summaryId) {
        Summary summary = summaryService.findByIdWithoutStats(summaryId);

        // 발행된 요약본에만 댓글 작성 가능
        if (summary.getPublishStatus() != PublishStatus.PUBLISHED) {
            log.error("발행되지 않은 요약본에 댓글 작성 시도: summaryId={}, status={}",
                    summaryId, summary.getPublishStatus());
            throw new UnpublishedSummaryCommentException();
        }

        return summary;
    }

    /**
     * 부모 댓글이 같은 요약본이 댓글인지 확인
     */
    private void validateParentComment(Long summeryId, Long parentCommentId, Comment parentComment) {
        if (parentComment.isNotEqualsSummaryId(summeryId)) {
            log.error("부모 댓글과 요약본 불일치: parentCommentId={}, summeryId={}", parentComment.getSummary().getId(),
                    summeryId);
            throw new InvalidParentCommentException(parentCommentId);
        }
    }

    /**
     * 댓글이 삭제되지 않았는지 확인
     */
    private void validateCommentNotDeleted(Comment comment) {
        if (comment.isDeleted()) {
            log.error("삭제된 댓글에 대한 작업 시도: commentId={}", comment.getId());
            throw new DeletedCommentException();
        }
    }

    /**
     * 댓글 소유권 확인 (작성자인지 확인)
     */
    private void validateCommentOwnership(Comment comment, String providerUid) {
        if (!comment.getMember().getProviderUid().equals(providerUid)) {
            log.error("댓글 소유권 없음: commentId={}, 요청자={}, 작성자={}",
                    comment.getId(), providerUid, comment.getMember().getProviderUid());
            throw new CommentAccessDeniedException();
        }
    }

    private void validateAction(String action) {
        if (action == null ||
                (!action.equalsIgnoreCase("like") && !action.equalsIgnoreCase("dislike"))) {
            throw new InvalidLikeActionException(action);
        }
    }

    private CommentLikeResponse processLike(Member member, Comment comment, String action) {
        Optional<CommentLike> existingLike = commentLikeRepository.findByMemberAndComment(member, comment);

        if (existingLike.isPresent() && "dislike".equalsIgnoreCase(action)) {
            // 좋아요 제거
            commentLikeRepository.delete(existingLike.get());
            comment.decreaseLikeCount();
            log.debug("댓글 좋아요 제거: commentId={}, memberId={}, newLikeCount={}",
                    comment.getId(), member.getId(), comment.getLikeCount());

        } else if (existingLike.isEmpty() && "like".equalsIgnoreCase(action)) {
            // 좋아요 추가
            CommentLike commentLike = CommentLike.of(member, comment);
            commentLikeRepository.save(commentLike);
            comment.increaseLikeCount();
            log.debug("댓글 좋아요 추가: commentId={}, memberId={}, newLikeCount={}",
                    comment.getId(), member.getId(), comment.getLikeCount());

        } else {
            // 중복 요청 처리
            if (existingLike.isPresent() && "like".equalsIgnoreCase(action)) {
                log.debug("이미 좋아요가 되어있는 댓글: commentId={}, memberId={}",
                        comment.getId(), member.getId());
            } else if (existingLike.isEmpty() && "dislike".equalsIgnoreCase(action)) {
                log.debug("좋아요가 되어있지 않은 댓글에 dislike 요청: commentId={}, memberId={}",
                        comment.getId(), member.getId());
            }
        }

        return CommentLikeResponse.from(action, comment.getLikeCount());
    }
}
