package joomidang.papersummary.common.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Getter
@MappedSuperclass
@EntityListeners(value = {AuditingEntityListener.class})
public class BaseTimeEntity {
    @CreatedDate
    @DateTimeFormat(iso = ISO.DATE_TIME)
    @Column(columnDefinition = "TIMESTAMP", nullable = false, updatable = false)
    protected LocalDateTime createdAt;

    @LastModifiedDate
    @DateTimeFormat(iso = ISO.DATE_TIME)
    @Column(columnDefinition = "TIMESTAMP")
    protected LocalDateTime updatedAt;
}
