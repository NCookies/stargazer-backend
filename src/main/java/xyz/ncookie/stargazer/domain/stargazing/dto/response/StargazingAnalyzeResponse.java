package xyz.ncookie.stargazer.domain.stargazing.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "별 관측 조건 분석 응답 DTO")
public record StargazingAnalyzeResponse(
	@Schema(description = "관측 날짜", example = "2025-12-19")
	String date,
	
	@Schema(description = "관측 시간", example = "22:00")
	String time,

	@Schema(description = "종합 점수 (0-100)", example = "85")
	int totalScore,
	
	@Schema(description = "점수 감점 사유 목록", example = "[\"구름량이 많음\", \"습도가 높음\"]")
	List<String> reasons,
	
	@Schema(description = "AI 한줄 평", example = "오늘 밤 별 관측하기 좋은 날씨입니다!")
	String aiComment,

	@Schema(description = "기상 정보")
	WeatherInfo weather,

	@Schema(description = "천문 정보")
	AstronomyInfo astronomy,

	@Schema(description = "광해 정보 (AI 추정)")
	LightPollutionInfo lightPollution
) {
	@Schema(description = "기상 정보")
	public record WeatherInfo(
		@Schema(description = "구름량 (%)", example = "20")
		int cloudIndex,
		
		@Schema(description = "습도 (%)", example = "60")
		int humidityIndex,
		
		@Schema(description = "가시도 등급", example = "매우 좋음")
		String visibilityText
	) {}

	@Schema(description = "천문 정보")
	public record AstronomyInfo(
		@Schema(description = "달 위상", example = "초승달")
		String moonPhase,
		
		@Schema(description = "일출 시간", example = "07:00")
		String sunrise,
		
		@Schema(description = "일몰 시간", example = "18:00")
		String sunset,
		
		@Schema(description = "월출 시간", example = "08:00")
		String moonrise,
		
		@Schema(description = "월몰 시간", example = "20:00")
		String moonset
	) {}

	@Schema(description = "광해 정보")
	public record LightPollutionInfo(
		@Schema(description = "보틀 등급 클래스", example = "Class 3")
		String bortleClass,
		
		@Schema(description = "밝기 수준", example = "낮음")
		String brightness,
		
		@Schema(description = "한계 등급", example = "6등급")
		String limitingMag
	) {}
}
