package xyz.ncookie.stargazer.global.util;

import org.springframework.stereotype.Component;

import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherResponse;

@Component
public class WeatherDataFactory {

	/**
	 * 에러 시 Fallback용 더미 날씨 데이터 생성
	 * 구름 100%로 설정하여 관측 불가 상태를 나타냄
	 */
	public OpenWeatherResponse createDummyWeather() {
		return new OpenWeatherResponse(
			new OpenWeatherResponse.Main(0.0, 50.0),
			new OpenWeatherResponse.Clouds(100), // 구름 100% (관측 불가)
			10000, // 시정 10km
			null
		);
	}
}