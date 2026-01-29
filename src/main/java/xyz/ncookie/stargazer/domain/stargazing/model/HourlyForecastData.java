package xyz.ncookie.stargazer.domain.stargazing.model;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 시간대별 관측 예보 도메인 모델
 */
public record HourlyForecastData(
	ZonedDateTime dateTime,      // 시간대
	int score,                    // 관측 점수
	List<String> reasons,         // 감점 사유
	String starGrade,             // 별 등급
	int cloudCover,               // 구름량 (%)
	String moonPhase,             // 달 위상
	RawAstronomyData astro        // 천문 데이터 (일출/일몰 등)
) {}
