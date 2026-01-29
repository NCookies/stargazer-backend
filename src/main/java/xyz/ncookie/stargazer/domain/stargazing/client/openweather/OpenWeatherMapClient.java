package xyz.ncookie.stargazer.domain.stargazing.client.openweather;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.global.exception.RateLimitExceededException;
import xyz.ncookie.stargazer.global.util.WeatherDataFactory;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenWeatherMapClient {

	@Value("${weather.api.key}")
	private String apiKey;

	private final RestTemplate restTemplate;

	private final WeatherDataFactory weatherDataFactory;

	private final Bucket openWeatherRateLimitBucket;

	/**
	 * OpenWeatherMap Weather API 원본 데이터 호출
	 * - 현재 날씨 데이터
	 * - 실제 HTTP 호출 직전에만 토큰 소비 (캐시와 무관)
	 */
	public OpenWeatherResponse fetchCurrentWeatherApi(double lat, double lon) {
		if (!openWeatherRateLimitBucket.tryConsume(1)) {
			throw new RateLimitExceededException();
		}
		String url = String.format(
			"https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric",
			lat, lon, apiKey
		);
		try {
			return restTemplate.getForObject(url, OpenWeatherResponse.class);
		} catch (Exception e) {
			log.error("Weather API 호출 실패", e);
			return weatherDataFactory.createDummyWeather();
		}
	}

	/**
	 * OpenWeatherMap Forecast API 원본 데이터 호출
	 * - 5일 3시간 간격의 예보 데이터
	 * - 동일 (lat, lon) 요청은 1시간 캐시로 외부 API 호출 최소화
	 * - 캐시 미스일 때만 메서드 본문이 실행되므로, 토큰은 실제 HTTP 호출 시에만 소비됨
	 */
	@Cacheable(value = "weatherForecast", key = "T(java.lang.String).format('%.4f-%.4f', #lat, #lon)")
	public OpenWeatherForecastResponse fetchForecastApi(double lat, double lon) {
		if (!openWeatherRateLimitBucket.tryConsume(1)) {
			throw new RateLimitExceededException();
		}
		String url = String.format(
			"https://api.openweathermap.org/data/2.5/forecast?lat=%f&lon=%f&appid=%s&units=metric",
			lat, lon, apiKey
		);
		try {
			return restTemplate.getForObject(url, OpenWeatherForecastResponse.class);
		} catch (Exception e) {
			log.error("Forecast API 호출 실패", e);
			return null;
		}
	}
}
