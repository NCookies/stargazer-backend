package xyz.ncookie.stargazer.domain.stargazing.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "별 관측 예보 응답 DTO")
public record StargazingForecastResponse(
	@Schema(description = "날짜별 예보 목록")
	List<DailyForecast> dailyForecasts
) {
	@Schema(description = "일별 예보")
	public record DailyForecast(
		@Schema(description = "날짜 (요일 포함)", example = "2025-05-20 (금)")
		String date,
		
		@Schema(description = "일출 시간", example = "07:00")
		String sunrise,
		
		@Schema(description = "일몰 시간", example = "18:00")
		String sunset,
		
		@Schema(description = "월출 시간", example = "08:00")
		String moonrise,
		
		@Schema(description = "월몰 시간", example = "20:00")
		String moonset,
		
		@Schema(description = "시간대별 예보 목록")
		List<HourlyForecast> hourlyForecasts
	) {}

	@Schema(description = "시간대별 예보")
	public record HourlyForecast(
		@Schema(description = "시간", example = "21:00")
		String time,
		
		@Schema(description = "관측 점수 (0-100)", example = "85")
		int score,
		
		@Schema(description = "감점 사유 목록", example = "[\"구름량이 많음\"]")
		List<String> reasons,
		
		@Schema(description = "별 등급", example = "4.5등급")
		String starGrade,
		
		@Schema(description = "구름량 (%)", example = "20")
		int cloudCover,
		
		@Schema(description = "달 위상", example = "초승달")
		String moonPhase
	) {}
}
