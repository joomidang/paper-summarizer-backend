package joomidang.papersummary.analysislog.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import joomidang.papersummary.common.audit.entity.BaseTimeEntity;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.paper.entity.Paper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 논문 분석 과정 로그를 저장하는 엔티티
 */
@Entity
@Table(name = "analysis_log", indexes = {
        @Index(name = "idx_paper_id", columnList = "paper_id"),
        @Index(name = "idx_member_id", columnList = "member_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_started_at", columnList = "started_at")
})
@Getter
@Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
public class AnalysisLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "paper_id")
    private Paper paper;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisTool tool;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private AnalysisSourceType sourceType;

    /**
     * 각 단계별 완료 처리
     */
    public void complete() {
        this.status = AnalysisStatus.SUCCESS;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    /**
     * 각 단계별 실패 처리
     *
     * @param errorMessage 실패 사유
     */
    public void fail(String errorMessage) {
        this.status = AnalysisStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 분석 단계 업데이트
     *
     * @param stage 새 분석 단계
     */
    public void updateStage(AnalysisStage stage) {
        this.stage = stage;
    }
}

