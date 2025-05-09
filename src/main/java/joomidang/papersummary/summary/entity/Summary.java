package joomidang.papersummary.summary.entity;

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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import joomidang.papersummary.common.audit.entity.BaseTimeEntity;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.paper.entity.Paper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "summary", indexes = {
        @Index(name = "idx_summary_member_id", columnList = "member_id"),
        @Index(name = "idx_summary_paper_id", columnList = "paper_id"),
        @Index(name = "idx_summary_publish_status", columnList = "publish_status")
})
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Summary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500, nullable = false)
    private String title;

    @Lob
    private String brief;

    @Column(name = "s3_key_md", length = 500, nullable = false)
    private String s3KeyMd;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", nullable = false)
    private PublishStatus publishStatus;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", unique = true)
    private Paper paper;

    public Long getSummaryId() {
        return id;
    }

    public Long getPaperId() {
        return paper.getId();
    }

    public boolean isNotSameMemberId(Long memberId) {
        return member.isNotSame(memberId);
    }

    public void publish(String title, String brief, String s3KeyMd) {
        this.title = title;
        this.brief = brief;
        this.s3KeyMd = s3KeyMd;
        this.publishStatus = PublishStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
    }
}
