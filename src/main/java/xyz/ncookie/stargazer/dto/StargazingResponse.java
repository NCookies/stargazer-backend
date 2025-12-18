package xyz.ncookie.stargazer.dto;

public record StargazingResponse(
	int score,                  // 0~100점
	String summary,             // "별 보기 아주 좋아요!"
	WeatherCondition weather,   // 날씨 정보
	AstronomyCondition astronomy // 천문 정보
) {
	public record WeatherCondition(double cloudCover, double humidity) {}
	public record AstronomyCondition(double moonIllumination, String moonPhase) {}
}
