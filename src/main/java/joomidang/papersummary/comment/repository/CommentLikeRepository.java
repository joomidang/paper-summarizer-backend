package joomidang.papersummary.comment.repository;

import java.util.Optional;
import joomidang.papersummary.comment.entity.Comment;
import joomidang.papersummary.comment.entity.CommentLike;
import joomidang.papersummary.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    Optional<CommentLike> findByMemberAndComment(Member member, Comment comment);

    boolean existsByMemberAndComment(Member member, Comment comment);
}
