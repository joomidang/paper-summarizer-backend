package joomidang.papersummary.visualcontent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.summary.entity.Summary;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "visual_content",
        indexes = {
                @Index(name = "idx_summary_id", columnList = "summary_id"),
                @Index(name = "idx_summary_position_unique", columnList = "summary_id, position", unique = true)
        }
)
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class VisualContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id")
    private Paper paper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "summary_id")
    private Summary summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisualContentType type;

    @Column(name = "storage_url", nullable = false, length = 500)
    private String storageUrl;

    @Column(nullable = false)
    private Integer position;

    public static VisualContent of(Paper paper, VisualContentType type, String storageUrl, Integer position) {
        return VisualContent.builder()
                .paper(paper)
                .type(type)
                .storageUrl(storageUrl)
                .position(position)
                .build();
    }

    public void connectToSummary(Summary summary) {
        this.summary = summary;
    }
}