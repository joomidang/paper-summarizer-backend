package joomidang.papersummary.common.config.redis;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
@EnableRedisRepositories(
        basePackages = {}, // Redis Repository가 있다면 여기에 패키지 명시
        // 빈 배열로 두어 Redis Repository 스캔하지 않음
        includeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = org.springframework.data.keyvalue.repository.KeyValueRepository.class
        ),
        // JPA Repository 및 다른 Repository 타입 제외
        excludeFilters = {
                @org.springframework.context.annotation.ComponentScan.Filter(
                        type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                        classes = org.springframework.data.jpa.repository.JpaRepository.class
                ),
                @org.springframework.context.annotation.ComponentScan.Filter(
                        type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                        classes = org.springframework.data.elasticsearch.repository.ElasticsearchRepository.class
                )
        }
)
public class RedisConfig {

    /**
     * ObjectMapper에 JavaTimeModule 등록 및 타입 정보 처리 설정
     */
    @Bean
    public GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // LocalDateTime 지원 추가

        // 타입 정보 직렬화 비활성화
        objectMapper.deactivateDefaultTyping();

        objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    /**
     * Redis CacheManager
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
    public CacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer serializer
    ) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("similarSummaries", defaultConfig.entryTtl(Duration.ofHours(24)))
                .withCacheConfiguration("embeddings", defaultConfig.entryTtl(Duration.ofDays(7)))
                .build();
    }

    /**
     * 로컬 캐시 매니저 (Redis 비활성 시)
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
    public CacheManager localCacheManager() {
        return new ConcurrentMapCacheManager("similarSummaries", "embeddings");
    }

    /**
     * RedisTemplate 설정
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer serializer
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}
