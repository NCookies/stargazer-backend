package xyz.ncookie.stargazer.domain.recommend.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "오늘 관측 추천 북마크 응답")
public record RecommendedBookmarkResponse(
	@Schema(description = "북마크 ID", example = "1")
	Long bookmarkId,

	@Schema(description = "북마크 이름", example = "제주도 별보기 명소")
	String name,

	@Schema(description = "위도", example = "33.4996")
	Double latitude,

	@Schema(description = "경도", example = "126.5312")
	Double longitude,

	@Schema(description = "주소", example = "제주특별자치도 제주시 애월읍")
	String address,

	@Schema(description = "관측 점수 (0-100)", example = "85")
	int score,

	@Schema(description = "감점 사유 목록", example = "[\"구름량이 많음\"]")
	List<String> reasons,

	@Schema(description = "별 등급", example = "4.5등급")
	String starGrade,

	@Schema(description = "구름량 (%)", example = "20")
	int cloudCover,

	@Schema(description = "달 위상", example = "초승달")
	String moonPhase,

	@Schema(description = "최적 관측 시간", example = "21:00")
	String bestTime
) {}
