package joomidang.papersummary.comment.service;

import joomidang.papersummary.comment.controller.response.CommentResponse;
import joomidang.papersummary.comment.entity.Comment;
import joomidang.papersummary.comment.exception.CommentAccessDeniedException;
import joomidang.papersummary.comment.exception.CommentNotFoundException;
import joomidang.papersummary.comment.exception.InvalidParentCommentException;
import joomidang.papersummary.comment.repository.CommentRepository;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.service.MemberService;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.service.SummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        log.debug("댓글 작성 완료 : commentId={}", savedComment.getContent());
        return CommentResponse.from(savedComment);
    }

    /**
     * 대댓글 작성
     */
    @Transactional
    public CommentResponse createReply(String providerUid, Long summeryId, Long parentCommentId, String content) {
        log.debug("대댓글 작성 시작: parentCommentId");
        Summary summary = validateSummaryForComment(summeryId);
        Comment parentComment = findCommentById(parentCommentId);

        validateParentComment(summeryId, parentCommentId, parentComment);

        Member member = memberService.findByProviderUid(providerUid);

        Comment replyComment = Comment.builder()
                .content(content)
                .summary(summary)
                .member(member)
                .parent(parentComment)
                .build();

        Comment savedReplyComment = commentRepository.save(replyComment);
        parentComment.addChild(savedReplyComment);
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
            throw new IllegalArgumentException("발행되지 않은 요약본에는 댓글을 작성할 수 없습니다.");
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
            throw new IllegalArgumentException("삭제된 댓글입니다.");
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

}
