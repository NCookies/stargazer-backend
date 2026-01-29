package xyz.ncookie.stargazer.domain.recommend.model;

import java.util.List;

import xyz.ncookie.stargazer.domain.bookmark.entity.Bookmark;
import xyz.ncookie.stargazer.domain.recommend.dto.response.RecommendedBookmarkItemResponse;

/**
 * 북마크와 관측 점수 정보를 담는 도메인 모델
 * Application Service 내부에서 점수 계산 및 정렬에 사용
 */
public record BookmarkScore(
	Bookmark bookmark,
	int score,
	List<String> reasons,
	String starGrade,
	int cloudCover,
	String moonPhase,
	String bestTime
) {

	/**
	 * BookmarkScore를 Response DTO로 변환
	 */
	public RecommendedBookmarkItemResponse toResponse() {
		return new RecommendedBookmarkItemResponse(
			bookmark.getId(),
			bookmark.getName(),
			bookmark.getLatitude(),
			bookmark.getLongitude(),
			bookmark.getAddress(),
			this.score(),
			this.reasons(),
			this.starGrade(),
			this.cloudCover(),
			this.moonPhase(),
			this.bestTime()
		);
	}
}
