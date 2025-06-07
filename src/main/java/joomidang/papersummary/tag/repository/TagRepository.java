package joomidang.papersummary.tag.repository;

import java.util.List;
import java.util.Optional;
import joomidang.papersummary.tag.entity.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String normalizedName);

    /**
     * 사용 횟수(usageCount)를 기준으로 내림차순 정렬하여 태그 목록 조회
     */
    List<Tag> findAllByOrderByUsageCountDesc(Pageable pageable);
}
