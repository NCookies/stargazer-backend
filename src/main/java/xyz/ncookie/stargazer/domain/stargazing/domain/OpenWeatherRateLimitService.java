package xyz.ncookie.stargazer.domain.stargazing.domain;

import java.time.Duration;

import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;

/**
 * 외부 API (OpenWeatherMap) 호출 속도를 제어하는 전역 서비스
 * Token Bucket 알고리즘을 사용
 */
@Slf4j
@Service
public class OpenWeatherRateLimitService {

	private final Bucket bucket;

	public OpenWeatherRateLimitService() {
		// ---------------------------------------------------------
		// 1. 대역폭(Bandwidth) 정의
		// ---------------------------------------------------------
		// Capacity: 버킷의 총 크기 (최대 60개 보관 가능)
		// Refill: 충전 속도 (1분에 60개 충전)
		//
		// Refill.greedy: "탐욕적" 충전 방식.
		// 1분에 60개라고 해서 1분 기다렸다가 60개를 쾅 주는 게 아니라,
		// 1초에 1개씩 꾸준히 채워줌. (트래픽 스파이크 방지에 더 유리함)
		Bandwidth limit = Bandwidth.classic(60, Refill.greedy(60, Duration.ofMinutes(1)));

		// 2. 버킷 생성
		this.bucket = Bucket.builder()
			.addLimit(limit)
			.build();
	}

	/**
	 * 토큰 1개 소비 시도 (Non-Blocking)
	 *
	 * @return true: 토큰 소비 성공 (API 호출 가능)
	 * false: 토큰 부족 (Rate Limit 초과, 호출 불가)
	 */
	public boolean tryConsume() {
		// tryConsume(1): 토큰 1개를 꺼낼 수 있으면 꺼내고 true 반환. 없으면 즉시 false 반환.
		boolean consumed = bucket.tryConsume(1);

		if (!consumed) {
			log.debug("Rate limit token exhausted. (Remaining tokens: {})", bucket.getAvailableTokens());
		}

		return consumed;
	}
}
