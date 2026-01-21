package xyz.ncookie.stargazer.global.security.redis;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisRepository {

	private final StringRedisTemplate redisTemplate;

	private static final String PREFIX = "RT:";

	public void save(String refreshToken, Long memberId, long expireMs) {
		redisTemplate.opsForValue()
			.set(PREFIX + refreshToken, String.valueOf(memberId), expireMs, TimeUnit.MILLISECONDS);
	}

	public Long find(String refreshToken) {
		String value = redisTemplate.opsForValue().get(PREFIX + refreshToken);

		if (value == null) {
			return null;
		}

		return Long.valueOf(value);
	}

	public void delete(String refreshToken) {
		redisTemplate.delete(PREFIX + refreshToken);
	}
}
