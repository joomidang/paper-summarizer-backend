package joomidang.papersummary.tag.repository;

import java.util.Optional;
import joomidang.papersummary.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String normalizedName);
}
