package xyz.ncookie.stargazer.domain.bookmark.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import xyz.ncookie.stargazer.domain.bookmark.entity.Bookmark;
import xyz.ncookie.stargazer.domain.bookmark.entity.BookmarkType;
import xyz.ncookie.stargazer.domain.spot.entity.ObservationSpot;

@Schema(description = "북마크 응답 DTO. type이 SPOT이면 spotId·좌표·주소는 명소 기준, CUSTOM이면 사용자 정의 장소 기준이며 spotId는 null.")
@Builder
public record BookmarkResponse(
	@Schema(description = "북마크 ID", example = "1")
	Long bookmarkId,

	@Schema(description = "북마크 타입", example = "SPOT", allowableValues = {"SPOT", "CUSTOM"})
	String type,

	@Schema(description = "관측지(명소) ID. SPOT 타입일 때만 존재, CUSTOM 타입이면 null.", example = "1")
	Long spotId,

	@Schema(description = "북마크 이름 (사용자 정의)", example = "강원도 대관령")
	String name,

	@Schema(description = "위도", example = "37.5665")
	Double latitude,

	@Schema(description = "경도", example = "126.9780")
	Double longitude,

	@Schema(description = "주소", example = "강원도 평창군 대관령면")
	String address,

	@Schema(description = "메모 (선택)", example = "겨울에 별 보기 좋음")
	String memo
) {

	public static BookmarkResponse from(Bookmark bookmark) {

		if (bookmark.getSpot() != null) {
			// 명소 정보 반환
			ObservationSpot spot = bookmark.getSpot();

			return BookmarkResponse.builder()
				.bookmarkId(bookmark.getId())
				.type(BookmarkType.SPOT.name())
				.spotId(spot.getId())
				.name(bookmark.getCustomName())
				.latitude(spot.getLatitude())
				.longitude(spot.getLongitude())
				.address(spot.getAddress())
				.memo(bookmark.getMemo())
				.build();
		} else {
			// 커스텀 정보 반환
			return BookmarkResponse.builder()
				.bookmarkId(bookmark.getId())
				.type(BookmarkType.CUSTOM.name())
				.name(bookmark.getCustomName())
				.latitude(bookmark.getLatitude())
				.longitude(bookmark.getLongitude())
				.address(bookmark.getAddress())
				.memo(bookmark.getMemo())
				.build();
		}
	}
}
