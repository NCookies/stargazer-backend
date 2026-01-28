package xyz.ncookie.stargazer.domain.stargazing.domain;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.stargazing.client.gemini.GeminiAnalysisClient;
import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherForecastResponse;
import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherMapClient;
import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherResponse;
import xyz.ncookie.stargazer.domain.stargazing.dto.mapper.OpenWeatherResponseMapper;
import xyz.ncookie.stargazer.domain.stargazing.engine.StargazingScoringEngine;
import xyz.ncookie.stargazer.domain.stargazing.model.RawAstronomyData;
import xyz.ncookie.stargazer.domain.stargazing.provider.WeatherDataProvider;
import xyz.ncookie.stargazer.domain.stargazing.model.GeminiAnalysisResult;
import xyz.ncookie.stargazer.domain.stargazing.model.StarAnalysisResult;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StargazingDomainService {

	private final WeatherDataProvider weatherDataProvider;
	private final StargazingScoringEngine scoringEngine;
	private final GeminiAnalysisClient geminiAnalysisClient;

	public OpenWeatherResponse fetchWeatherData(double lat, double lon, ZonedDateTime targetDateTime) {
		return weatherDataProvider.fetchWeatherData(lat, lon, targetDateTime);
	}

	public StarAnalysisResult calculateScore(double lat, double lon, ZonedDateTime targetDateTime, OpenWeatherResponse weatherData) {
		return scoringEngine.calculateScore(lat, lon, targetDateTime, weatherData);
	}

	public GeminiAnalysisResult getAnalysis(
		int score,
		List<String> reasons,
		double lat,
		double lon,
		OpenWeatherResponse weatherData,
		RawAstronomyData astro,
		int bortleClass
	) {
		return geminiAnalysisClient.getAnalysis(score, reasons, lat, lon, weatherData, astro, bortleClass);
	}
}
