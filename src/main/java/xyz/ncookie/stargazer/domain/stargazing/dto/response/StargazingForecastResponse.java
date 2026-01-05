package xyz.ncookie.stargazer.domain.stargazing.dto.response;

import java.util.List;

public record StargazingForecastResponse(
	List<DailyForecast> dailyForecasts // 날짜별로 묶음
) {
	public record DailyForecast(
		String date,           // "2025-05-20 (금)"
		List<HourlyForecast> hourlyForecasts // 그 날 밤의 시간대별 데이터
	) {}

	public record HourlyForecast(
		String time,           // "21:00"
		int score,             // 점수
		List<String> reasons,  // 상세 감점 사유 (프론트 표시용)
		String starGrade,      // "4.5등급" (별 등급) - 계산값
		int cloudCover,        // 구름
		String moonPhase       // 달 모양
	) {}
}
