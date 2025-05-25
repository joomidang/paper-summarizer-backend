package joomidang.papersummary.summary.entity;

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
import joomidang.papersummary.member.entity.Member;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "summary_like", indexes = {
        @Index(name = "idx_summary_like_member_id", columnList = "member_id"),
        @Index(name = "idx_summary_like_summary_id", columnList = "summary_id"),
        @Index(name = "idx_summary_like_unique", columnList = "member_id, summary_id", unique = true)
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SummaryLike extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "summary_id", nullable = false)
    private Summary summary;

    public static SummaryLike of(Member member, Summary summary) {
        return SummaryLike.builder()
                .member(member)
                .summary(summary)
                .build();
    }
}
