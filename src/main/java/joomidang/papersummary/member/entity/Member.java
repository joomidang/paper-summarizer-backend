package joomidang.papersummary.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import joomidang.papersummary.common.audit.entity.BaseTimeEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보를 저장하는 엔티티
 */
@Entity
@Table(name = "member", indexes = {
        @Index(name = "idx_member_email", columnList = "email"),
        @Index(name = "idx_member_name", columnList = "name"),
        @Index(name = "idx_member_auth_provider_uid", columnList = "auth_provider, provider_uid")
})
@Getter
@Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "provider_uid", length = 120)
    private String providerUid;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateProfile(String username, String email, String profileImage) {
        this.name = username;
        this.email = email;
        this.profileImage = profileImage;
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isNotSame(Long requesterId) {
        return !id.equals(requesterId);
    }
}
