package joomidang.papersummary.comment.repository;

import java.util.List;
import joomidang.papersummary.comment.entity.Comment;
import joomidang.papersummary.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query(value = "SELECT c FROM Comment c JOIN FETCH c.member m WHERE c.summary.id = :summaryId "
            + "AND c.parent IS NULL AND c.isDeleted = false "
            + "ORDER BY c.createdAt ASC",
            countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.summary.id = :summaryId "
                    + "AND c.parent IS NULL AND c.isDeleted = false")
    Page<Comment> findRootCommentsBySummaryIdWithPaging(@Param("summaryId") Long summaryId, Pageable pageable);

    @Query("SELECT c FROM Comment c JOIN FETCH c.member m WHERE c.parent.id IN :parentIds "
            + "AND c.isDeleted = false ORDER BY c.createdAt ASC")
    List<Comment> findChildrenByParentIds(@Param("parentIds") List<Long> parentIds);

    @Query(value = "SELECT c FROM Comment c JOIN FETCH c.member m JOIN FETCH c.summary s WHERE c.member =:member "
            + "AND c.isDeleted = false ORDER BY c.createdAt DESC",
            countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.member =:member AND c.isDeleted = false")
    Page<Comment> findByMemberAndDeletedFalseWithSummary(@Param("member") Member member, Pageable pageable);
}
