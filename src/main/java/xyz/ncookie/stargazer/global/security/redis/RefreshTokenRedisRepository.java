package xyz.ncookie.stargazer.global.security.redis;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisRepository {

	private final RedisTemplate<String, Long> redisTemplate;

	private static final String PREFIX = "RT:";

	public void save(String refreshToken, Long memberId, long expireMs) {
		redisTemplate.opsForValue()
			.set(PREFIX + refreshToken, memberId, expireMs, TimeUnit.MILLISECONDS);
	}

	public Long find(String refreshToken) {
		return redisTemplate.opsForValue().get(PREFIX + refreshToken);
	}

	public void delete(String refreshToken) {
		redisTemplate.delete(PREFIX + refreshToken);
	}
}
