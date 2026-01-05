package xyz.ncookie.stargazer.domain.stargazing.model;

import java.util.List;

public record StarAnalysisResult(
	int score,             // 최종 점수
	List<String> reasons,  // 상세 감점 사유 (프론트 표시용)
	int bortleClass,       // 광해 등급
	RawAstronomyData astro // 천문 데이터 (달 위상 등)
) {}
