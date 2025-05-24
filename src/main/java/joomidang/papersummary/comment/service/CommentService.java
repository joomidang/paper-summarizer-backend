package joomidang.papersummary.comment.service;

import joomidang.papersummary.comment.controller.response.CommentResponse;
import joomidang.papersummary.comment.entity.Comment;
import joomidang.papersummary.comment.repository.CommentRepository;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.service.MemberService;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.service.SummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final SummaryService summaryService;
    private final MemberService memberService;

    /**
     * 댓글 작성
     */
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
}
