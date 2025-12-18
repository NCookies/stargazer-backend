package xyz.ncookie.stargazer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StargazingResponse(
	int totalScore,          // 종합 점수
	String aiComment,        // AI 한줄 평

	// 1. 기상 정보 (파란 카드)
	WeatherInfo weather,

	// 2. 천문 정보 (노란 카드)
	AstronomyInfo astronomy,

	// 3. 광해 정보 (보라색 카드) - AI가 추정
	LightPollutionInfo lightPollution
) {
	public record WeatherInfo(
		int cloudIndex,         // 구름량 (%)
		int humidityIndex,      // 습도 (%)
		String visibilityText   // "매우 좋음", "보통" 등
	) {}

	public record AstronomyInfo(
		String moonPhase,       // "초승달"
		String moonRiseTime,    // "23:45"
		String sunsetTime       // "19:32"
	) {}

	public record LightPollutionInfo(
		String bortleClass,     // "Class 3"
		String brightness,      // "낮음", "높음"
		String limitingMag      // "6등급"
	) {}
}
