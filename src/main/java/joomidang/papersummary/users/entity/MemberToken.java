package joomidang.papersummary.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 인증 토큰 정보를 저장하는 엔티티
 */
@Entity
@Table(name = "member_token", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_token_expires_at", columnList = "token_expires_at")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @Column(name = "access_token_enc", columnDefinition = "text")
    private String accessTokenEnc;

    @Column(name = "refresh_token_enc", columnDefinition = "text")
    private String refreshTokenEnc;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isExpired() {
        return tokenExpiresAt != null && tokenExpiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * 토큰 만료 시간 업데이트
     *
     * @param expiresAt 새로운 만료 시간
     */
    public void updateExpiresAt(LocalDateTime expiresAt) {
        this.tokenExpiresAt = expiresAt;
    }
}