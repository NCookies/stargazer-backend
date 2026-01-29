package xyz.ncookie.stargazer.domain.recommend.dto.response;

import java.util.List;

public record RecommendedBookmarkResponse(
	List<RecommendedBookmarkItemResponse> items, 	// 추천 결과
	int totalRequested,                      		// 분석 요청한 전체 개수
	int analyzedCount,                       		// 실제 분석 성공 개수
	boolean isPartialResult                  		// 일부가 스킵되었는지 여부
) {}
