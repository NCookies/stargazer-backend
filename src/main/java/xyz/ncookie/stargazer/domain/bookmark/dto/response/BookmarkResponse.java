package xyz.ncookie.stargazer.domain.bookmark.dto.response;

import lombok.Builder;
import xyz.ncookie.stargazer.domain.bookmark.entity.Bookmark;
import xyz.ncookie.stargazer.domain.bookmark.entity.BookmarkType;
import xyz.ncookie.stargazer.domain.spot.entity.ObservationSpot;

@Builder
public record BookmarkResponse(
	Long bookmarkId,
	String type,
	Long spotId,
	String name,
	Double latitude,
	Double longitude,
	String address,
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
