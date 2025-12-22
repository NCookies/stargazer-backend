package xyz.ncookie.stargazer.dto;

import java.util.Collections;
import java.util.List;

public record GeminiRequest(List<Content> contents) {
	public record Content(List<Part> parts) {}
	public record Part(String text) {}

	// 사용하기 편하게 만드는 생성자 팩토리
	public static GeminiRequest of(String prompt) {
		return new GeminiRequest(
			Collections.singletonList(new Content(Collections.singletonList(new Part(prompt))))
		);
	}
}
