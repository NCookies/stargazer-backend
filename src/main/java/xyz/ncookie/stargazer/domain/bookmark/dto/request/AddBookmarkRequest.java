package xyz.ncookie.stargazer.domain.bookmark.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import xyz.ncookie.stargazer.domain.bookmark.entity.BookmarkType;

public record AddBookmarkRequest(

	@NotNull(message = "북마크 타입은 필수입니다.")
	BookmarkType type,

	Long spotId,

	@Size(max = 50, message = "북마크 이름은 최대 50자까지 입력 가능합니다.")
	@NotBlank(message = "북마크 이름은 필수입니다.")
	String name,

	Double latitude,
	Double longitude,

	String address,

	@Size(max = 500, message = "메모는 최대 500자까지 입력 가능합니다.")
	String memo
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
