package joomidang.papersummary.common.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@MappedSuperclass
public class BaseTimeEntity {
    @Column(nullable = false, updatable = false)
    protected LocalDateTime createdAt;

    @Column
    protected LocalDateTime updatedAt;

    @Column(columnDefinition = "TINYINT(1) DEFAULT 0", nullable = false)
    protected boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = null;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
