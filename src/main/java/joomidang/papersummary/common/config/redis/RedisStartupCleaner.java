package joomidang.papersummary.common.config.redis;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("local") // 🔐 "local" 프로파일에서만 동작
public class RedisStartupCleaner {

    private final RedisTemplate<String, Object> redisTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void clearEmbeddingAndRecommendationCache() {
        log.info("[local] Redis 캐시 초기화 시작");

        deleteByPattern("embedding:*");
        deleteByPattern("similar_summaries:*");
    }

    private void deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Redis 키 삭제 완료: pattern={}, deletedKeys={}", pattern, keys.size());
            } else {
                log.info("Redis 키 없음: pattern={}", pattern);
            }
        } catch (Exception e) {
            log.warn("Redis 키 삭제 실패: pattern={}, error={}", pattern, e.getMessage());
        }
    }
}
