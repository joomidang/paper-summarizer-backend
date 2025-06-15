package joomidang.papersummary.comment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
import joomidang.papersummary.member.entity.AuthProvider;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.entity.Role;
import joomidang.papersummary.member.service.MemberService;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.service.SummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class CommentServiceTest {
    private CommentService commentService;
    private CommentRepository commentRepository;
    private SummaryService summaryService;
    private MemberService memberService;
    private CommentLikeRepository commentLikeRepository;
    private StatsEventPublisher statsEventPublisher;

    private Member testMember;
    private Member anotherMember;
    private Summary publishedSummary;
    private Summary draftSummary;
    private Comment parentComment;
    private Comment childComment;
    private Comment deletedComment;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        summaryService = mock(SummaryService.class);
        memberService = mock(MemberService.class);
        commentRepository = mock(CommentRepository.class);
        summaryService = mock(SummaryService.class);
        memberService = mock(MemberService.class);
        commentLikeRepository = mock(CommentLikeRepository.class);
        statsEventPublisher = mock(StatsEventPublisher.class);

        commentService = new CommentService(commentRepository, summaryService, memberService, commentLikeRepository,
                statsEventPublisher);

        // 테스트용 Member 생성
        testMember = Member.builder()
                .id(1L)
                .email("test@example.com")
                .name("TestUser")
                .providerUid("test-provider-uid")
                .authProvider(AuthProvider.GITHUB)
                .role(Role.USER)
                .build();

        anotherMember = Member.builder()
                .id(2L)
                .email("another@example.com")
                .name("AnotherUser")
                .providerUid("another-provider-uid")
                .authProvider(AuthProvider.GITHUB)
                .role(Role.USER)
                .build();

        // 테스트용 Summary 생성
        publishedSummary = Summary.builder()
                .id(1L)
                .title("Test Summary")
                .publishStatus(PublishStatus.PUBLISHED)
                .member(testMember)
                .build();

        draftSummary = Summary.builder()
                .id(2L)
                .title("Draft Summary")
                .publishStatus(PublishStatus.DRAFT)
                .member(testMember)
                .build();

        // 테스트용 Comment 생성
        parentComment = Comment.builder()
                .id(1L)
                .content("Parent comment")
                .summary(publishedSummary)
                .member(testMember)
                .build();

        childComment = Comment.builder()
                .id(2L)
                .content("Child comment")
                .summary(publishedSummary)
                .member(testMember)
                .parent(parentComment)
                .build();

        deletedComment = Comment.builder()
                .id(3L)
                .content("Deleted comment")
                .summary(publishedSummary)
                .member(testMember)
                .build();
        deletedComment.softDelete();
    }

    // =================== createComment 테스트 ===================

    @Test
    @DisplayName("댓글 작성 성공 테스트")
    void createCommentSuccess() {
        // given
        String providerUid = "test-provider-uid";
        Long summaryId = 1L;
        String content = "Test comment";

        when(summaryService.findByIdWithoutStats(summaryId)).thenReturn(publishedSummary);
        when(memberService.findByProviderUid(providerUid)).thenReturn(testMember);
        when(commentRepository.save(any(Comment.class))).thenReturn(parentComment);

        // when
        CommentResponse result = commentService.createComment(providerUid, summaryId, content);

        // then
        assertNotNull(result);
        assertEquals("Parent comment", result.content());
        verify(summaryService, times(1)).findByIdWithoutStats(summaryId);
        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(commentRepository, times(1)).save(any(Comment.class));

        // Comment 엔티티 생성 검증
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(commentCaptor.capture());
        Comment capturedComment = commentCaptor.getValue();
        assertEquals(content, capturedComment.getContent());
        assertEquals(publishedSummary, capturedComment.getSummary());
        assertEquals(testMember, capturedComment.getMember());
    }

    // =================== createReply 테스트 ===================

    @Test
    @DisplayName("대댓글 작성 성공 테스트")
    void createReplySuccess() {
        // given
        String providerUid = "test-provider-uid";
        Long summaryId = 1L;
        Long parentCommentId = 1L;
        String content = "Reply comment";

        when(summaryService.findByIdWithoutStats(summaryId)).thenReturn(publishedSummary);
        when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parentComment));
        when(memberService.findByProviderUid(providerUid)).thenReturn(testMember);
        when(commentRepository.save(any(Comment.class))).thenReturn(childComment);

        // when
        CommentResponse result = commentService.createReply(providerUid, summaryId, parentCommentId, content);

        // then
        assertNotNull(result);
        assertEquals("Child comment", result.content());
        verify(summaryService, times(1)).findByIdWithoutStats(summaryId);
        verify(commentRepository, times(1)).findById(parentCommentId);
        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(commentRepository, times(1)).save(any(Comment.class));

        // Comment 엔티티 생성 검증
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(commentCaptor.capture());
        Comment capturedComment = commentCaptor.getValue();
        assertEquals(content, capturedComment.getContent());
        assertEquals(publishedSummary, capturedComment.getSummary());
        assertEquals(testMember, capturedComment.getMember());
        assertEquals(parentComment, capturedComment.getParent());
    }

    @Test
    @DisplayName("발행되지 않은 요약본에 대댓글 작성 시 예외 발생")
    void createReplyOnDraftSummary() {
        // given
        String providerUid = "test-provider-uid";
        Long summaryId = 2L;
        Long parentCommentId = 1L;
        String content = "Reply comment";

        when(summaryService.findByIdWithoutStats(summaryId)).thenReturn(draftSummary);

        // when & then
        assertThrows(UnpublishedSummaryCommentException.class, () -> {
            commentService.createReply(providerUid, summaryId, parentCommentId, content);
        });

        verify(summaryService, times(1)).findByIdWithoutStats(summaryId);
        verify(commentRepository, never()).findById(anyLong());
        verify(memberService, never()).findByProviderUid(any());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("존재하지 않는 부모 댓글에 대댓글 작성 시 예외 발생")
    void createReplyWithNonExistentParent() {
        // given
        String providerUid = "test-provider-uid";
        Long summaryId = 1L;
        Long parentCommentId = 999L;
        String content = "Reply comment";

        when(summaryService.findByIdWithoutStats(summaryId)).thenReturn(publishedSummary);
        when(commentRepository.findById(parentCommentId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(CommentNotFoundException.class, () -> {
            commentService.createReply(providerUid, summaryId, parentCommentId, content);
        });

        verify(summaryService, times(1)).findByIdWithoutStats(summaryId);
        verify(commentRepository, times(1)).findById(parentCommentId);
        verify(memberService, never()).findByProviderUid(any());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("다른 요약본의 댓글을 부모로 대댓글 작성 시 예외 발생")
    void createReplyWithInvalidParent() {
        // given
        String providerUid = "test-provider-uid";
        Long summaryId = 1L;
        Long parentCommentId = 1L;
        String content = "Reply comment";

        // 다른 요약본에 속한 부모 댓글
        Comment differentSummaryComment = Comment.builder()
                .id(1L)
                .content("Different summary comment")
                .summary(draftSummary) // 다른 요약본
                .member(testMember)
                .build();

        when(summaryService.findByIdWithoutStats(summaryId)).thenReturn(publishedSummary);
        when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(differentSummaryComment));

        // when & then
        assertThrows(InvalidParentCommentException.class, () -> {
            commentService.createReply(providerUid, summaryId, parentCommentId, content);
        });

        verify(summaryService, times(1)).findByIdWithoutStats(summaryId);
        verify(commentRepository, times(1)).findById(parentCommentId);
        verify(memberService, never()).findByProviderUid(any());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    // =================== updateComment 테스트 ===================

    @Test
    @DisplayName("댓글 수정 성공 테스트")
    void updateCommentSuccess() {
        // given
        String providerUid = "test-provider-uid";
        Long commentId = 1L;
        String newContent = "Updated comment";

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(parentComment));

        // when
        CommentResponse result = commentService.updateComment(providerUid, commentId, newContent);

        // then
        assertNotNull(result);
        verify(commentRepository, times(1)).findById(commentId);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 수정 시 예외 발생")
    void updateCommentNotFound() {
        // given
        String providerUid = "test-provider-uid";
        Long commentId = 999L;
        String newContent = "Updated comment";

        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(CommentNotFoundException.class, () -> {
            commentService.updateComment(providerUid, commentId, newContent);
        });

        verify(commentRepository, times(1)).findById(commentId);
    }

    @Test
    @DisplayName("삭제된 댓글 수정 시 예외 발생")
    void updateDeletedComment() {
        // given
        String providerUid = "test-provider-uid";
        Long commentId = 3L;
        String newContent = "Updated comment";

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(deletedComment));

        // when & then
        assertThrows(DeletedCommentException.class, () -> {
            commentService.updateComment(providerUid, commentId, newContent);
        });

        verify(commentRepository, times(1)).findById(commentId);
    }

    @Test
    @DisplayName("다른 사용자의 댓글 수정 시 예외 발생")
    void updateCommentAccessDenied() {
        // given
        String providerUid = "another-provider-uid";
        Long commentId = 1L;
        String newContent = "Updated comment";

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(parentComment));

        // when & then
        assertThrows(CommentAccessDeniedException.class, () -> {
            commentService.updateComment(providerUid, commentId, newContent);
        });

        verify(commentRepository, times(1)).findById(commentId);
    }

    // =================== deleteComment 테스트 ===================

    @Test
    @DisplayName("댓글 삭제 성공 테스트")
    void deleteCommentSuccess() {
        // given
        String providerUid = "test-provider-uid";
        Long commentId = 1L;

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(parentComment));

        // when
        commentService.deleteComment(providerUid, commentId);

        // then
        verify(commentRepository, times(1)).findById(commentId);
        // 댓글이 삭제되었는지 확인
        assert(parentComment.isDeleted());
    }

    @Test
    @DisplayName("부모 댓글 삭제 시 대댓글도 함께 삭제되는지 테스트")
    void deleteCommentWithChildrenSuccess() {
        // given
        String providerUid = "test-provider-uid";
        Long commentId = 1L;

        // 부모 댓글과 자식 댓글 설정
        Comment parent = Comment.builder()
                .id(1L)
                .content("Parent comment")
                .summary(publishedSummary)
                .member(testMember)
                .build();

        Comment child1 = Comment.builder()
                .id(2L)
                .content("Child comment 1")
                .summary(publishedSummary)
                .member(testMember)
                .parent(parent)
                .build();

        Comment child2 = Comment.builder()
                .id(3L)
                .content("Child comment 2")
                .summary(publishedSummary)
                .member(testMember)
                .parent(parent)
                .build();

        // 자식 댓글의 자식 댓글 (손자 댓글)
        Comment grandchild = Comment.builder()
                .id(4L)
                .content("Grandchild comment")
                .summary(publishedSummary)
                .member(testMember)
                .parent(child1)
                .build();

        // 부모-자식 관계 설정
        parent.getChildren().add(child1);
        parent.getChildren().add(child2);
        child1.getChildren().add(grandchild);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(parent));

        // when
        commentService.deleteComment(providerUid, commentId);

        // then
        verify(commentRepository, times(1)).findById(commentId);

        // 부모 댓글이 삭제되었는지 확인
        assert(parent.isDeleted());

        // 모든 자식 댓글이 삭제되었는지 확인
        assert(child1.isDeleted());
        assert(child2.isDeleted());

        // 손자 댓글도 삭제되었는지 확인
        assert(grandchild.isDeleted());
    }

    @Test
    @DisplayName("존재하지 않는 댓글 삭제 시 예외 발생")
    void deleteCommentNotFound() {
        // given
        String providerUid = "test-provider-uid";
        Long commentId = 999L;

        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(CommentNotFoundException.class, () -> {
            commentService.deleteComment(providerUid, commentId);
        });

        verify(commentRepository, times(1)).findById(commentId);
    }

    @Test
    @DisplayName("이미 삭제된 댓글 삭제 시 예외 발생")
    void deleteAlreadyDeletedComment() {
        // given
        String providerUid = "test-provider-uid";
        Long commentId = 3L;

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(deletedComment));

        // when & then
        assertThrows(DeletedCommentException.class, () -> {
            commentService.deleteComment(providerUid, commentId);
        });

        verify(commentRepository, times(1)).findById(commentId);
    }

    @Test
    @DisplayName("다른 사용자의 댓글 삭제 시 예외 발생")
    void deleteCommentAccessDenied() {
        // given
        String providerUid = "another-provider-uid";
        Long commentId = 1L;

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(parentComment));

        // when & then
        assertThrows(CommentAccessDeniedException.class, () -> {
            commentService.deleteComment(providerUid, commentId);
        });

        verify(commentRepository, times(1)).findById(commentId);
    }

    // =================== getCommentsBySummaryWithPaging 테스트 ===================

    @Test
    @DisplayName("요약본 댓글 페이징 조회 성공 테스트")
    void getCommentsBySummaryWithPagingSuccess() {
        // given
        Long summaryId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        List<Comment> rootComments = Arrays.asList(parentComment);
        Page<Comment> rootCommentsPage = new PageImpl<>(rootComments, pageable, 1);
        List<Comment> childComments = Arrays.asList(childComment);

        when(summaryService.findByIdWithoutStats(summaryId)).thenReturn(publishedSummary);
        when(commentRepository.findRootCommentsBySummaryIdWithPaging(summaryId, pageable))
                .thenReturn(rootCommentsPage);
        when(commentRepository.findChildrenByParentIds(Arrays.asList(1L))).thenReturn(childComments);

        // when
        CommentListResponse result = commentService.getCommentsBySummaryWithPaging(summaryId, pageable);

        // then
        assertNotNull(result);
        verify(summaryService, times(1)).findByIdWithoutStats(summaryId);
        verify(commentRepository, times(1)).findRootCommentsBySummaryIdWithPaging(summaryId, pageable);
        verify(commentRepository, times(1)).findChildrenByParentIds(Arrays.asList(1L));
    }

    @Test
    @DisplayName("자식 댓글이 없는 경우 페이징 조회 테스트")
    void getCommentsBySummaryWithPagingNoChildren() {
        // given
        Long summaryId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        List<Comment> rootComments = Arrays.asList(parentComment);
        Page<Comment> rootCommentsPage = new PageImpl<>(rootComments, pageable, 1);

        when(summaryService.findByIdWithoutStats(summaryId)).thenReturn(publishedSummary);
        when(commentRepository.findRootCommentsBySummaryIdWithPaging(summaryId, pageable))
                .thenReturn(rootCommentsPage);
        when(commentRepository.findChildrenByParentIds(Arrays.asList(1L))).thenReturn(Collections.emptyList());

        // when
        CommentListResponse result = commentService.getCommentsBySummaryWithPaging(summaryId, pageable);

        // then
        assertNotNull(result);
        verify(summaryService, times(1)).findByIdWithoutStats(summaryId);
        verify(commentRepository, times(1)).findRootCommentsBySummaryIdWithPaging(summaryId, pageable);
        verify(commentRepository, times(1)).findChildrenByParentIds(Arrays.asList(1L));
    }

    @Test
    @DisplayName("빈 페이지 조회 테스트")
    void getCommentsBySummaryWithPagingEmptyPage() {
        // given
        Long summaryId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Page<Comment> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(summaryService.findByIdWithoutStats(summaryId)).thenReturn(publishedSummary);
        when(commentRepository.findRootCommentsBySummaryIdWithPaging(summaryId, pageable))
                .thenReturn(emptyPage);

        // when
        CommentListResponse result = commentService.getCommentsBySummaryWithPaging(summaryId, pageable);

        // then
        assertNotNull(result);
        verify(summaryService, times(1)).findByIdWithoutStats(summaryId);
        verify(commentRepository, times(1)).findRootCommentsBySummaryIdWithPaging(summaryId, pageable);
        verify(commentRepository, never()).findChildrenByParentIds(any());
    }

    // =================== getCommentById 테스트 ===================

    @Test
    @DisplayName("댓글 단건 조회 성공 테스트")
    void getCommentByIdSuccess() {
        // given
        Long commentId = 1L;

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(parentComment));

        // when
        Comment result = commentService.getCommentById(commentId);

        // then
        assertNotNull(result);
        assertEquals(parentComment.getId(), result.getId());
        assertEquals(parentComment.getContent(), result.getContent());
        verify(commentRepository, times(1)).findById(commentId);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 단건 조회 시 예외 발생")
    void getCommentByIdNotFound() {
        // given
        Long commentId = 999L;

        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(CommentNotFoundException.class, () -> {
            commentService.getCommentById(commentId);
        });

        verify(commentRepository, times(1)).findById(commentId);
    }

    @Test
    @DisplayName("삭제된 댓글 단건 조회 시 예외 발생")
    void getCommentByIdDeleted() {
        // given
        Long commentId = 3L;

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(deletedComment));

        // when & then
        assertThrows(DeletedCommentException.class, () -> {
            commentService.getCommentById(commentId);
        });

        verify(commentRepository, times(1)).findById(commentId);
    }

    // =================== getCommentsByMember 테스트 ===================

    @Test
    @DisplayName("사용자 댓글 조회 성공 테스트 - 삭제되지 않은 요약본의 삭제되지 않은 댓글만 조회")
    void getCommentsByMemberSuccess() {
        // given
        String providerUid = "test-provider-uid";
        Pageable pageable = PageRequest.of(0, 10);

        List<Comment> userComments = Arrays.asList(parentComment, childComment);
        Page<Comment> userCommentsPage = new PageImpl<>(userComments, pageable, 2);

        when(memberService.findByProviderUid(providerUid)).thenReturn(testMember);
        when(commentRepository.findByMemberAndDeletedFalseWithSummary(testMember, pageable))
                .thenReturn(userCommentsPage);

        // when
        CommentListResponse result = commentService.getCommentsByMember(providerUid, pageable);

        // then
        assertNotNull(result);
        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(commentRepository, times(1)).findByMemberAndDeletedFalseWithSummary(testMember, pageable);
    }

    @Test
    @DisplayName("사용자 댓글이 없는 경우 조회 테스트 - 삭제되지 않은 요약본의 삭제되지 않은 댓글만 조회")
    void getCommentsByMemberEmpty() {
        // given
        String providerUid = "test-provider-uid";
        Pageable pageable = PageRequest.of(0, 10);

        Page<Comment> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(memberService.findByProviderUid(providerUid)).thenReturn(testMember);
        when(commentRepository.findByMemberAndDeletedFalseWithSummary(testMember, pageable))
                .thenReturn(emptyPage);

        // when
        CommentListResponse result = commentService.getCommentsByMember(providerUid, pageable);

        // then
        assertNotNull(result);
        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(commentRepository, times(1)).findByMemberAndDeletedFalseWithSummary(testMember, pageable);
    }

    // =================== likeComment 테스트 ===================

    @Test
    @DisplayName("댓글 좋아요 추가 성공 테스트")
    void likeCommentSuccess() throws Exception {
        // given
        String providerUid = "test-provider-uid";
        Long commentId = 1L;
        String action = "like";

        when(memberService.findByProviderUid(providerUid)).thenReturn(testMember);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(parentComment));
        when(commentLikeRepository.findByMemberAndComment(testMember, parentComment))
                .thenReturn(Optional.empty()); // 기존 좋아요 없음
        when(commentLikeRepository.save(any(CommentLike.class))).thenReturn(mock(CommentLike.class));

        // when
        CompletableFuture<CommentLikeResponse> future = commentService.likeComment(providerUid, commentId, action);
        CommentLikeResponse result = future.get();

        // then
        assertNotNull(result);
        assertEquals(true, result.liked());
        assertEquals(1, result.likeCount()); // increaseLikeCount 호출로 1이 됨

        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(commentRepository, times(1)).findById(commentId);
        verify(commentLikeRepository, times(1)).findByMemberAndComment(testMember, parentComment);
        verify(commentLikeRepository, times(1)).save(any(CommentLike.class));
        verify(commentLikeRepository, never()).delete(any(CommentLike.class));

        // CommentLike 엔티티 생성 검증
        ArgumentCaptor<CommentLike> commentLikeCaptor = ArgumentCaptor.forClass(CommentLike.class);
        verify(commentLikeRepository).save(commentLikeCaptor.capture());
        CommentLike capturedCommentLike = commentLikeCaptor.getValue();
        assertEquals(testMember, capturedCommentLike.getMember());
        assertEquals(parentComment, capturedCommentLike.getComment());
    }

    @Test
    @DisplayName("댓글 좋아요 제거 성공 테스트")
    void dislikeCommentSuccess() throws Exception {
        // given
        String providerUid = "test-provider-uid";
        Long commentId = 1L;
        String action = "dislike";

        CommentLike existingLike = CommentLike.of(testMember, parentComment);

        when(memberService.findByProviderUid(providerUid)).thenReturn(testMember);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(parentComment));
        when(commentLikeRepository.findByMemberAndComment(testMember, parentComment))
                .thenReturn(Optional.of(existingLike)); // 기존 좋아요 있음

        // when
        CompletableFuture<CommentLikeResponse> future = commentService.likeComment(providerUid, commentId, action);
        CommentLikeResponse result = future.get();

        // then
        assertNotNull(result);
        assertEquals(false, result.liked());
        assertEquals(-1, result.likeCount()); // decreaseLikeCount 호출로 -1이 됨

        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(commentRepository, times(1)).findById(commentId);
        verify(commentLikeRepository, times(1)).findByMemberAndComment(testMember, parentComment);
        verify(commentLikeRepository, times(1)).delete(existingLike);
        verify(commentLikeRepository, never()).save(any(CommentLike.class));
    }

    @Test
    @DisplayName("이미 좋아요된 댓글에 다시 좋아요 요청 시 중복 처리")
    void likeCommentAlreadyLiked() throws Exception {
        // given
        String providerUid = "test-provider-uid";
        Long commentId = 1L;
        String action = "like";

        CommentLike existingLike = CommentLike.of(testMember, parentComment);

        when(memberService.findByProviderUid(providerUid)).thenReturn(testMember);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(parentComment));
        when(commentLikeRepository.findByMemberAndComment(testMember, parentComment))
                .thenReturn(Optional.of(existingLike)); // 이미 좋아요 있음

        // when
        CompletableFuture<CommentLikeResponse> future = commentService.likeComment(providerUid, commentId, action);
        CommentLikeResponse result = future.get();

        // then
        assertNotNull(result);
        assertEquals(true, result.liked());
        assertEquals(0, result.likeCount()); // 좋아요 수 변경 없음

        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(commentRepository, times(1)).findById(commentId);
        verify(commentLikeRepository, times(1)).findByMemberAndComment(testMember, parentComment);
        verify(commentLikeRepository, never()).save(any(CommentLike.class));
        verify(commentLikeRepository, never()).delete(any(CommentLike.class));
    }

    @Test
    @DisplayName("좋아요되지 않은 댓글에 dislike 요청 시 중복 처리")
    void dislikeCommentNotLiked() throws Exception {
        // given
        String providerUid = "test-provider-uid";
        Long commentId = 1L;
        String action = "dislike";

        when(memberService.findByProviderUid(providerUid)).thenReturn(testMember);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(parentComment));
        when(commentLikeRepository.findByMemberAndComment(testMember, parentComment))
                .thenReturn(Optional.empty()); // 좋아요 없음

        // when
        CompletableFuture<CommentLikeResponse> future = commentService.likeComment(providerUid, commentId, action);
        CommentLikeResponse result = future.get();

        // then
        assertNotNull(result);
        assertEquals(false, result.liked());
        assertEquals(0, result.likeCount()); // 좋아요 수 변경 없음

        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(commentRepository, times(1)).findById(commentId);
        verify(commentLikeRepository, times(1)).findByMemberAndComment(testMember, parentComment);
        verify(commentLikeRepository, never()).save(any(CommentLike.class));
        verify(commentLikeRepository, never()).delete(any(CommentLike.class));
    }

    @Test
    @DisplayName("잘못된 action 값으로 댓글 좋아요 요청 시 예외 발생")
    void likeCommentInvalidAction() {
        // given
        String providerUid = "test-provider-uid";
        Long commentId = 1L;
        String action = "invalid_action";

        when(memberService.findByProviderUid(providerUid)).thenReturn(testMember);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(parentComment));

        // when & then
        assertThrows(InvalidLikeActionException.class, () -> {
            CompletableFuture<CommentLikeResponse> future = commentService.likeComment(providerUid, commentId, action);
            future.get();
        });

        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(commentRepository, times(1)).findById(commentId);
        verify(commentLikeRepository, never()).findByMemberAndComment(any(), any());
        verify(commentLikeRepository, never()).save(any(CommentLike.class));
        verify(commentLikeRepository, never()).delete(any(CommentLike.class));
    }

    @Test
    @DisplayName("null action 값으로 댓글 좋아요 요청 시 예외 발생")
    void likeCommentNullAction() {
        // given
        String providerUid = "test-provider-uid";
        Long commentId = 1L;
        String action = null;

        when(memberService.findByProviderUid(providerUid)).thenReturn(testMember);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(parentComment));

        // when & then
        assertThrows(InvalidLikeActionException.class, () -> {
            CompletableFuture<CommentLikeResponse> future = commentService.likeComment(providerUid, commentId, action);
            future.get();
        });

        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(commentRepository, times(1)).findById(commentId);
        verify(commentLikeRepository, never()).findByMemberAndComment(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 댓글에 좋아요 요청 시 예외 발생")
    void likeCommentNotFound() {
        // given
        String providerUid = "test-provider-uid";
        Long commentId = 999L;
        String action = "like";

        when(memberService.findByProviderUid(providerUid)).thenReturn(testMember);
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(CommentNotFoundException.class, () -> {
            CompletableFuture<CommentLikeResponse> future = commentService.likeComment(providerUid, commentId, action);
            future.get();
        });

        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(commentRepository, times(1)).findById(commentId);
        verify(commentLikeRepository, never()).findByMemberAndComment(any(), any());
    }

    @Test
    @DisplayName("삭제된 댓글에 좋아요 요청 시 예외 발생")
    void likeDeletedComment() {
        // given
        String providerUid = "test-provider-uid";
        Long commentId = 3L;
        String action = "like";

        when(memberService.findByProviderUid(providerUid)).thenReturn(testMember);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(deletedComment));

        // when & then
        assertThrows(DeletedCommentException.class, () -> {
            CompletableFuture<CommentLikeResponse> future = commentService.likeComment(providerUid, commentId, action);
            future.get();
        });

        verify(memberService, times(1)).findByProviderUid(providerUid);
        verify(commentRepository, times(1)).findById(commentId);
        verify(commentLikeRepository, never()).findByMemberAndComment(any(), any());
    }

    @Test
    @DisplayName("대소문자 구분 없이 action 처리 테스트 - LIKE")
    void likeCommentCaseInsensitiveLike() throws Exception {
        // given
        String providerUid = "test-provider-uid";
        Long commentId = 1L;
        String action = "LIKE"; // 대문자

        when(memberService.findByProviderUid(providerUid)).thenReturn(testMember);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(parentComment));
        when(commentLikeRepository.findByMemberAndComment(testMember, parentComment))
                .thenReturn(Optional.empty());
        when(commentLikeRepository.save(any(CommentLike.class))).thenReturn(mock(CommentLike.class));

        // when
        CompletableFuture<CommentLikeResponse> future = commentService.likeComment(providerUid, commentId, action);
        CommentLikeResponse result = future.get();

        // then
        assertNotNull(result);
        assertEquals(true, result.liked());
        verify(commentLikeRepository, times(1)).save(any(CommentLike.class));
    }

    @Test
    @DisplayName("대소문자 구분 없이 action 처리 테스트 - DISLIKE")
    void likeCommentCaseInsensitiveDislike() throws Exception {
        // given
        String providerUid = "test-provider-uid";
        Long commentId = 1L;
        String action = "DISLIKE"; // 대문자

        CommentLike existingLike = CommentLike.of(testMember, parentComment);

        when(memberService.findByProviderUid(providerUid)).thenReturn(testMember);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(parentComment));
        when(commentLikeRepository.findByMemberAndComment(testMember, parentComment))
                .thenReturn(Optional.of(existingLike));

        // when
        CompletableFuture<CommentLikeResponse> future = commentService.likeComment(providerUid, commentId, action);
        CommentLikeResponse result = future.get();

        // then
        assertNotNull(result);
        assertEquals(false, result.liked());
        verify(commentLikeRepository, times(1)).delete(existingLike);
    }
}
