package joomidang.papersummary.tag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import joomidang.papersummary.common.audit.entity.BaseTimeEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tag", indexes = {
        @Index(name = "idx_tag_name", columnList = "name", unique = true),
        @Index(name = "idx_tag_usage_count", columnList = "usage_count")
})
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Tag extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 20)
    private String name;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Integer usageCount = 0;

    public static Tag create(String name) {
        return Tag.builder()
                .name(name)
                .usageCount(0)
                .build();
    }

    public static boolean isValidTagName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        String normalized = name.trim();

        // 길이 검증 (1-20자)
        if (normalized.length() > 20) {
            return false;
        }

        // 허용된 문자만 포함하는지 검증 (한글, 영문, 숫자, 공백, 하이픈, 언더스코어)
        return normalized.matches("^[가-힣a-zA-Z0-9\\s\\-_]+$");
    }

    public void increaseUsageCount() {
        this.usageCount++;
    }

    public void decreaseUsageCount() {
        if (this.usageCount > 0) {
            this.usageCount--;
        }
    }
}