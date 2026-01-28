package xyz.ncookie.stargazer.domain.bookmark.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import xyz.ncookie.stargazer.domain.bookmark.entity.Bookmark;
import xyz.ncookie.stargazer.domain.bookmark.entity.BookmarkType;
import xyz.ncookie.stargazer.domain.spot.entity.ObservationSpot;

@Schema(description = "북마크 응답")
@Builder
public record BookmarkResponse(
	@Schema(description = "북마크 ID", example = "1")
	Long bookmarkId,

	@Schema(
		description = "북마크 타입 (BookmarkType과 동일: CUSTOM | SPOT)",
		example = "SPOT",
		allowableValues = {"CUSTOM", "SPOT"}
	)
	String type,

	@Schema(
		description = "관측지 ID (SPOT 타입일 때만 존재, CUSTOM 타입일 때는 null)",
		example = "1",
		nullable = true
	)
	Long spotId,

	@Schema(description = "북마크 이름", example = "제주도 별보기 명소")
	String name,

	@Schema(description = "위도", example = "33.4996")
	Double latitude,

	@Schema(description = "경도", example = "126.5312")
	Double longitude,

	@Schema(description = "주소", example = "제주특별자치도 제주시 애월읍")
	String address,

	@Schema(description = "메모", example = "밤하늘이 정말 맑아서 별이 잘 보이는 곳입니다.", nullable = true)
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
