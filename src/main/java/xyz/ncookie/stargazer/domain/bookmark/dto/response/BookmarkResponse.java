package xyz.ncookie.stargazer.domain.bookmark.dto.response;

import lombok.Builder;
import xyz.ncookie.stargazer.domain.bookmark.entity.Bookmark;
import xyz.ncookie.stargazer.domain.spot.entity.ObservationSpot;

@Builder
public record BookmarkResponse(
	Long bookmarkId,
	Long spotId,
	String name,
	Double latitude,
	Double longitude,
	String address,
	String type
) {

	public static BookmarkResponse from(Bookmark bookmark) {

		if (bookmark.getSpot() != null) {
			// 명소 정보 반환
			ObservationSpot spot = bookmark.getSpot();

			return BookmarkResponse.builder()
				.bookmarkId(bookmark.getId())
				.spotId(spot.getId())
				.name(bookmark.getCustomName())
				.latitude(spot.getLatitude())
				.longitude(spot.getLongitude())
				.address(spot.getAddress())
				.type("SPOT")
				.build();
		} else {
			// 커스텀 정보 반환
			return BookmarkResponse.builder()
				.bookmarkId(bookmark.getId())
				.name(bookmark.getCustomName())
				.latitude(bookmark.getLatitude())
				.longitude(bookmark.getLongitude())
				.address(bookmark.getAddress())
				.type("CUSTOM")
				.build();
		}
	}
}
