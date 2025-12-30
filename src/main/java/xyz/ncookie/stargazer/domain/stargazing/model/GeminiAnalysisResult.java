package xyz.ncookie.stargazer.domain.stargazing.model;

// Gemini AI 분석 결과 (JSON 파싱 포함)
public record GeminiAnalysisResult(
	int finalScore,
	String comment,
	String bortleClass,
	String brightness,
	String limitingMag
) {}
