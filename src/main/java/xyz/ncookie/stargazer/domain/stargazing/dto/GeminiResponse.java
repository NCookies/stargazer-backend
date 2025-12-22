package xyz.ncookie.stargazer.domain.stargazing.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponse(List<Candidate> candidates) {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Candidate(Content content) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Content(List<Part> parts) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Part(String text) {}

	// 텍스트만 쏙 뽑아내는 헬퍼 메소드
	public String getText() {
		if (candidates == null || candidates.isEmpty()) return "AI 분석 불가";
		return candidates.get(0).content().parts().get(0).text();
	}
}
