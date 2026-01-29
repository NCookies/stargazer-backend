package xyz.ncookie.stargazer.global.exception;

/**
 * OpenWeatherMap API 분당 한도 초과 시, 실제 HTTP 호출 직전에 발생.
 * 호출부(예: RecommendApplicationService)에서 catch 후 해당 요청만 스킵 처리.
 */
public class RateLimitExceededException extends RuntimeException {

	public RateLimitExceededException() {
		super("OpenWeatherMap API rate limit exceeded.");
	}
}
