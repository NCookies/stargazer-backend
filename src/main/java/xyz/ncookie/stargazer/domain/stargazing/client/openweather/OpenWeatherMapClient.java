package xyz.ncookie.stargazer.domain.stargazing.client.openweather;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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

	public String getAddressName(double lat, double lon) {
		// OpenWeatherMap Reverse Geocoding API (무료)
		String url = String.format(
			"http://api.openweathermap.org/geo/1.0/reverse?lat=%f&lon=%f&limit=1&appid=%s",
			lat, lon, apiKey
		);

		try {
			// 응답용 임시 Record (내부 클래스로 정의)
			@JsonIgnoreProperties(ignoreUnknown = true)
			record GeoResult(String name, String country, String state) {} // state가 'Gyeonggi-do' 같은 정보

			GeoResult[] results = restTemplate.getForObject(url, GeoResult[].class);
			if (results != null && results.length > 0) {
				GeoResult r = results[0];
				// 예: "Yangpyeong-gun, KR" 형태로 반환
				return (r.name() != null ? r.name() : "") +
					(r.state() != null ? ", " + r.state() : "") +
					", " + r.country();
			}
		} catch (Exception e) {
			log.error("주소 변환 실패", e);
		}
		return "Unknown Location";
	}
}

