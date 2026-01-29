package xyz.ncookie.stargazer.global.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import xyz.ncookie.stargazer.global.cache.TwoLevelCacheManager;

@Configuration
public class CacheConfig {

	private static final RedisSerializationContext.SerializationPair<String> KEY_SERIALIZER =
		RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer());
	private static final RedisSerializationContext.SerializationPair<Object> VALUE_SERIALIZER =
		RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer());

	/**
	 * 이중 캐시: L1(로컬 Caffeine 5분) + L2(Redis)
	 * - weatherForecast: L2 1시간 TTL
	 * - bortleZone: L2 10분 TTL
	 */
	@Bean
	public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		// L1: 로컬 캐시, 짧은 TTL로 응답 속도 확보
		CaffeineCacheManager l1 = new CaffeineCacheManager("weatherForecast", "bortleZone");
		l1.setCacheSpecification("maximumSize=500, expireAfterWrite=5m");

		// L2: Redis, 재시작·다중 인스턴스 대응
		RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
			.entryTtl(Duration.ofMinutes(10))
			.serializeKeysWith(KEY_SERIALIZER)
			.serializeValuesWith(VALUE_SERIALIZER)
			.disableCachingNullValues();

		Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
			"weatherForecast",
			RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofHours(1))
				.serializeKeysWith(KEY_SERIALIZER)
				.serializeValuesWith(VALUE_SERIALIZER)
				.disableCachingNullValues()
		);

		RedisCacheManager l2 = RedisCacheManager.builder(connectionFactory)
			.cacheDefaults(defaultConfig)
			.withInitialCacheConfigurations(cacheConfigurations)
			.build();

		return new TwoLevelCacheManager(l1, l2);
	}
}
