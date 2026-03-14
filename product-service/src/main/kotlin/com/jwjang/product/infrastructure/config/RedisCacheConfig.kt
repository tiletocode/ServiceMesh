package com.jwjang.product.infrastructure.config


import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching
class RedisCacheConfig {

    private lateinit var cacheManagerInstance: RedisCacheManager

    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): RedisCacheManager {
        // GenericJackson2JsonRedisSerializer 기본 생성자: @class 타입 정보를 자동 포함/처리
        val jsonSerializer = GenericJackson2JsonRedisSerializer()

        val cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
            )
            .disableCachingNullValues()

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(cacheConfig)
            .build()
            .also { cacheManagerInstance = it }
    }

    /** 서비스 재시작 시 PageImpl 역직렬화 오류 방지를 위해 캐시 초기화 */
    @EventListener(ApplicationReadyEvent::class)
    fun clearCacheOnStartup() {
        runCatching {
            cacheManagerInstance.cacheNames.forEach { cacheManagerInstance.getCache(it)?.clear() }
        }
    }
}
