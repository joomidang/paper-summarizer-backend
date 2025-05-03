package joomidang.papersummary.visualcontent.repository;

import joomidang.papersummary.visualcontent.entity.VisualContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisualContentRepository extends JpaRepository<VisualContent, Long> {
}
