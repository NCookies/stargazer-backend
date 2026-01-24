package xyz.ncookie.stargazer.domain.bookmark.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import xyz.ncookie.stargazer.domain.bookmark.entity.BookmarkType;

public record AddBookmarkRequest(

	@NotNull(message = "북마크 타입은 필수입니다.")
	BookmarkType type,

	Long spotId,

	@NotBlank(message = "북마크 이름은 필수입니다.")
	String name,

	Double latitude,
	Double longitude,

	String address
) {

	@AssertTrue(message = "SPOT 타입은 spotId가 필수입니다.")
	private boolean isSpotValid() {

		if (type == BookmarkType.SPOT) {
			return spotId != null;
		}
		return true;
	}

	@AssertTrue(message = "CUSTOM 타입은 좌표(latitude, longitude)가 필수입니다.")
	private boolean isCustomValid() {

		if (type == BookmarkType.CUSTOM) {
			return latitude != null && longitude != null;
		}
		return true;
	}
}
