package joomidang.papersummary.tag.repository;

import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.tag.entity.SummaryTag;
import joomidang.papersummary.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SummaryTagRepository extends JpaRepository<SummaryTag, Long> {
    void deleteBySummary(Summary summary);

    boolean existsBySummaryAndTag(Summary summary, Tag tag);
}
