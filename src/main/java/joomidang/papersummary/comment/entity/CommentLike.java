package joomidang.papersummary.comment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import joomidang.papersummary.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comment_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_comment_like_member_comment",
                columnNames = {"member_id", "comment_id"}
        ),
        indexes = {
                @Index(name = "idx_comment_like_member_id", columnList = "member_id"),
                @Index(name = "idx_comment_like_comment_id", columnList = "comment_id"),
                @Index(name = "idx_comment_like_created_at", columnList = "created_at")
        })
@Getter
@Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
public class CommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public static CommentLike of(Member member, Comment comment) {
        return CommentLike.builder()
                .member(member)
                .comment(comment)
                .build();
    }

    /**
     * 해당 댓글 좋아요가 특정 사용자의 것인지 확인
     */
    public boolean isLikedBy(Long memberId) {
        return this.member.getId().equals(memberId);
    }

    public Long getCommentId() {
        return this.comment.getId();
    }

    /**
     * 사용자 ID 반환
     */
    public Long getMemberId() {
        return this.member.getId();
    }
}
