package xyz.ncookie.stargazer.domain.stargazing.model;

public record StarAnalysisResult(
	int finalScore,          // 최종 점수 (광해 반영됨)
	int weatherScore,        // 순수 기상 점수
	int bortleClass,         // 광해 등급
	RawAstronomyData astro   // 천문 데이터 (달, 일몰 등)
) {}
