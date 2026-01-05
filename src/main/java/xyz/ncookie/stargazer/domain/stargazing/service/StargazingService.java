package xyz.ncookie.stargazer.domain.stargazing.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.shredzone.commons.suncalc.SunPosition;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.stargazing.client.gemini.GeminiAnalysisClient;
import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherForecastResponse;
import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherMapClient;
import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherResponse;
import xyz.ncookie.stargazer.domain.stargazing.dto.response.StargazingForecastResponse;
import xyz.ncookie.stargazer.domain.stargazing.dto.request.StargazingRequest;
import xyz.ncookie.stargazer.domain.stargazing.dto.response.StargazingAnalyzeResponse;
import xyz.ncookie.stargazer.domain.stargazing.dto.mapper.OpenWeatherResponseMapper;
import xyz.ncookie.stargazer.domain.stargazing.engine.StargazingScoringEngine;
import xyz.ncookie.stargazer.domain.stargazing.model.RawAstronomyData;
import xyz.ncookie.stargazer.domain.stargazing.provider.WeatherDataProvider;
import xyz.ncookie.stargazer.domain.stargazing.model.GeminiAnalysisResult;
import xyz.ncookie.stargazer.domain.stargazing.enums.BortleGrade;
import xyz.ncookie.stargazer.domain.stargazing.enums.MoonPhase;
import xyz.ncookie.stargazer.domain.stargazing.model.StarAnalysisResult;
import xyz.ncookie.stargazer.domain.stargazing.enums.VisibilityGrade;

@Service
@Slf4j
@RequiredArgsConstructor
public class StargazingService {

	private final GeminiAnalysisClient geminiAnalysisClient;
	private final OpenWeatherMapClient weatherMapClient;

	private final WeatherDataProvider weatherDataProvider;
	private final StargazingScoringEngine scoringEngine;

	private final OpenWeatherResponseMapper openWeatherResponseMapper;

	/**
	 * 특정 시점(현재 또는 미래)의 관측 적합도 상세 분석
	 */
	public StargazingAnalyzeResponse getAnalyze(StargazingRequest request) {
		log.info("Analyzing stargazing request {}", request);

		// 파싱
		ZonedDateTime targetDateTime = ZonedDateTime.of(
			request.date(),
			request.time(),
			ZoneId.of("Asia/Seoul")
		);
		// ZonedDateTime targetDateTime = ZonedDateTime.of(
		// 	LocalDate.parse("2025-12-20"),
		// 	LocalTime.parse("21:00"),
		// 	ZoneId.of("Asia/Seoul")
		// );

		// 외부 데이터 수집
		OpenWeatherResponse weatherData = weatherDataProvider.fetchWeatherData(request.lat(), request.lon(), targetDateTime);

		StarAnalysisResult result = scoringEngine.calculateScore(request.lat(), request.lon(), targetDateTime, weatherData);

		GeminiAnalysisResult aiResult = geminiAnalysisClient.getAnalysis(
			result.score(),
			result.reasons(),
			request.lat(),
			request.lon(),
			weatherData,
			result.astro(),
			result.bortleClass()
		);

		return new StargazingAnalyzeResponse(
			targetDateTime.toLocalDate().toString(),
			targetDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
			result.score(),
			result.reasons(),
			aiResult.comment(),
			new StargazingAnalyzeResponse.WeatherInfo(
				weatherData.clouds().all(),
				(int) weatherData.main().humidity(),
				VisibilityGrade.from(weatherData.visibility()).getLabel()
			),
			new StargazingAnalyzeResponse.AstronomyInfo(
				MoonPhase.calculate(result.astro().moonPhaseDegree()).name(),
				result.astro().sunrise(),
				result.astro().sunset(),
				result.astro().moonrise(),
				result.astro().moonset()
			),
			new StargazingAnalyzeResponse.LightPollutionInfo(
				"Class " + result.bortleClass(),
				BortleGrade.from(result.bortleClass()).getBrightnessText(),
				BortleGrade.from(result.bortleClass()).getLimitingMagText()
			)
		);
	}

	/**
	 * 주간 예보 조회 (5일 / 3시간 간격)
	 */
	public StargazingForecastResponse getForecast(double lat, double lon) {

		Map<String, List<StargazingForecastResponse.HourlyForecast>> groupedData = new LinkedHashMap<>();
		Map<String, RawAstronomyData> dailyAstroMap = new HashMap<>();

		OpenWeatherForecastResponse rawData = weatherMapClient.fetchForecastApi(lat, lon);

		if (rawData == null || rawData.list() == null) {
			return new StargazingForecastResponse(List.of());
		}

		for (OpenWeatherForecastResponse.Item item : rawData.list()) {
			ZonedDateTime itemTime = ZonedDateTime.ofInstant(
				java.time.Instant.ofEpochSecond(item.dt()), ZoneId.of("Asia/Seoul")
			);

			// 태양 고도 체크
			SunPosition sunPos = SunPosition.compute().at(lat, lon).on(itemTime).execute();
			if (sunPos.getAltitude() > -6.0) continue; // 낮이면 스킵

			OpenWeatherResponse weatherResponse = openWeatherResponseMapper.toWeatherResponse(item);

			StarAnalysisResult result = scoringEngine.calculateScore(lat, lon, itemTime, weatherResponse);

			String dateKey = itemTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			dailyAstroMap.putIfAbsent(dateKey, result.astro());

			StargazingForecastResponse.HourlyForecast hourlyDto = new StargazingForecastResponse.HourlyForecast(
				itemTime.format(DateTimeFormatter.ofPattern("HH:mm")),
				result.score(),
				result.reasons(),
				String.format("%.1f등급", 6.0 - (item.clouds().all() / 20.0)),
				item.clouds().all(),
				MoonPhase.calculate(result.astro().moonPhaseDegree()).name()
			);

			// 날짜별 그룹화
			groupedData.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(hourlyDto);
		}

		List<StargazingForecastResponse.DailyForecast> dailyList = groupedData.entrySet().stream()
			.map(entry -> {
				String date = entry.getKey();
				RawAstronomyData astroData = dailyAstroMap.get(date);

				String sunrise = astroData.sunrise();
				String sunset = astroData.sunset();
				String moonrise = astroData.moonrise();
				String moonset = astroData.moonset();

				return new StargazingForecastResponse.DailyForecast(
					date,
					sunrise,
					sunset,
					moonrise,
					moonset,
					entry.getValue()
				);
			})
			.toList();

		return new StargazingForecastResponse(dailyList);
	}
}
