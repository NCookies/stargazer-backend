package xyz.ncookie.stargazer.domain.stargazing.client.openweather;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.global.util.WeatherDataFactory;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenWeatherMapClient {

	@Value("${weather.api.key}")
	private String apiKey;

	private final RestTemplate restTemplate;

	private final WeatherDataFactory weatherDataFactory;

	/**
	 * OpenWeatherMap Weather API 원본 데이터 호출
	 * - 현재 날씨 데이터
	 */
	public OpenWeatherResponse fetchCurrentWeatherApi(double lat, double lon) {
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
	 */
	public OpenWeatherForecastResponse fetchForecastApi(double lat, double lon) {
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

