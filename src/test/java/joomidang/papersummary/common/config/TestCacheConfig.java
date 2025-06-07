package joomidang.papersummary.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 테스트용 캐시 설정 - 간단한 메모리 캐싱 사용
 */
@TestConfiguration
@Profile("test")
public class TestCacheConfig {

    /**
     * 테스트용 캐시 매니저 - Redis 설정과 충돌하지 않도록 조건부로 활성화
     * spring.data.redis.enabled=false 일 때만 활성화됨
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
    public CacheManager testCacheManager() {
        // NoOpCacheManager 대신 ConcurrentMapCacheManager 사용
        // 캐싱은 수행하되 테스트 환경에 적합한 간단한 인메모리 캐싱 사용
        return new ConcurrentMapCacheManager("similarSummaries", "embeddings");
    }
}
