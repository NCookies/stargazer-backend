package xyz.ncookie.stargazer.domain.stargazing.provider;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherMapClient;
import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherForecastResponse;
import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherResponse;
import xyz.ncookie.stargazer.domain.stargazing.dto.mapper.OpenWeatherResponseMapper;
import xyz.ncookie.stargazer.global.util.WeatherDataFactory;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherDataProvider {

	private final OpenWeatherMapClient openWeatherMapClient;

	private final OpenWeatherResponseMapper openWeatherResponseMapper;
	private final WeatherDataFactory weatherDataFactory;

	/**
	 * 날씨 데이터 확보 전략
	 * - TargetTime이 현재와 가까우면: Current Weather API (/weather)
	 * - TargetTime이 미래면: Forecast API (/forecast) 호출 후 가장 가까운 시간대 추출
	 */
	public OpenWeatherResponse fetchWeatherData(double lat, double lon, ZonedDateTime targetDateTime) {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));

		// "지금"과 차이가 1시간 이내라면 -> 실시간 날씨 (/weather) 사용
		if (Math.abs(java.time.Duration.between(now, targetDateTime).toMinutes()) < 60) {
			return openWeatherMapClient.fetchCurrentWeatherApi(lat, lon);
		}

		// 미래의 특정 시간이라면 -> 예보 데이터 (/forecast) 사용
		return selectWeatherFromForecast(lat, lon, targetDateTime);
	}

	private OpenWeatherResponse selectWeatherFromForecast(double lat, double lon, ZonedDateTime targetTime) {
		// [공통 메서드 호출] Forecast API 데이터 가져오기
		OpenWeatherForecastResponse forecast = openWeatherMapClient.fetchForecastApi(lat, lon);

		if (forecast == null || forecast.list() == null) {
			return weatherDataFactory.createDummyWeather();
		}

		// 가장 가까운 시간대의 데이터 찾기 (Nearest Neighbor Search)
		OpenWeatherForecastResponse.Item bestMatch = null;
		long minDiff = Long.MAX_VALUE;

		for (OpenWeatherForecastResponse.Item item : forecast.list()) {
			ZonedDateTime itemTime = ZonedDateTime.ofInstant(
				java.time.Instant.ofEpochSecond(item.dt()), ZoneId.of("Asia/Seoul")
			);

			long diff = Math.abs(java.time.Duration.between(itemTime, targetTime).toMinutes());
			if (diff < minDiff) {
				minDiff = diff;
				bestMatch = item;
			}
		}

		if (bestMatch != null) {
			return openWeatherResponseMapper.toWeatherResponse(bestMatch);
		}

		return weatherDataFactory.createDummyWeather();
	}
}
