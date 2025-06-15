package joomidang.papersummary.tag.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import joomidang.papersummary.common.audit.entity.BaseTimeEntity;
import joomidang.papersummary.summary.entity.Summary;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "summary_tag", indexes = {
        @Index(name = "idx_summary_tag_summary_id", columnList = "summary_id"),
        @Index(name = "idx_summary_tag_tag_id", columnList = "tag_id"),
        @Index(name = "idx_summary_tag_unique", columnList = "summary_id, tag_id", unique = true)
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SummaryTag extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "summary_id", nullable = false)
    private Summary summary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public static SummaryTag of(Summary summary, Tag tag) {
        return SummaryTag.builder()
                .summary(summary)
                .tag(tag)
                .build();
    }
}
