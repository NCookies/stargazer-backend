package xyz.ncookie.stargazer.global.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

/**
 * OpenWeatherMap API 분당 60회 제한용 Bucket.
 * 실제 HTTP 호출 직전(캐시 미스 시)에만 토큰을 소비하도록 클라이언트에서 사용.
 */
@Configuration
public class OpenWeatherRateLimitConfig {

	public static final int CAPACITY_PER_MINUTE = 60;

	@Bean
	public Bucket openWeatherRateLimitBucket() {
		Bandwidth limit = Bandwidth.classic(
			CAPACITY_PER_MINUTE,
			Refill.intervally(CAPACITY_PER_MINUTE, Duration.ofMinutes(1))
		);
		return Bucket.builder()
			.addLimit(limit)
			.build();
	}
}
