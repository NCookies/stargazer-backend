package xyz.ncookie.stargazer.global.security.redis;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisRepository {

	private final RedisTemplate<String, String> redisTemplate;

	private static final String PREFIX = "RT:";

	public void save(Long memberId, String refreshToken, long expireMs) {

		redisTemplate.opsForValue()
			.set(PREFIX + memberId, refreshToken, expireMs, TimeUnit.MILLISECONDS);
	}

	public String find(Long memberId) {

		return redisTemplate.opsForValue().get(PREFIX + memberId);
	}

	public void delete(Long memberId) {

		redisTemplate.delete(PREFIX + memberId);
	}
}
