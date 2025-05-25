package joomidang.papersummary.summary.repository;

import java.util.Optional;
import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.summary.entity.Summary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, Long> {
    @Query("SELECT s FROM Summary s WHERE s.id = :id")
    Optional<Summary> findByIdWithoutStats(@Param("id") Long id);

    @Query("SELECT s FROM Summary s JOIN FETCH s.summaryStats WHERE s.id=:id")
    Optional<Summary> findByIdWithStats(@Param("id") Long id);

    boolean existsByPaper(Paper paper);

    @Query(value = "SELECT s FROM Summary s WHERE s.member.id = :memberId AND s.isDeleted = false",
           countQuery = "SELECT COUNT(s) FROM Summary s WHERE s.member.id = :memberId AND s.isDeleted = false")
    Page<Summary> findByMemberIdWithStats(@Param("memberId") Long memberId, Pageable pageable);

//    Page<Summary> findSummariesByMemberIdWithlikes
}
