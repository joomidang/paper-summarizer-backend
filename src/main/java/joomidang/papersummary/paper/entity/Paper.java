package joomidang.papersummary.paper.entity;

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
import joomidang.papersummary.common.audit.entity.BaseTimeEntity;
import joomidang.papersummary.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 논문 정보를 저장하는 엔티티
 */
@Slf4j
@Entity
@Table(name = "paper", indexes = {
        @Index(name = "idx_paper_title", columnList = "title"),
        @Index(name = "idx_paper_member_id", columnList = "member_id")
})
@Getter
@Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
public class Paper extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String title;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;


    /**
     * 논문 상태 업데이트
     *
     * @param status 새로운 상태
     */
    public void updateStatus(Status status) {
        this.status = status;
    }

    /**
     * 논문 삭제 처리
     */
    public void delete() {
        this.isDeleted = true;
    }

    public boolean hasNotPermission(Long requesterId) {
        return member.isNotSame(requesterId);
    }

    public void updateTitle(String title) {
        this.title = title;
    }
}