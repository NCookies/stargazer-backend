package xyz.ncookie.stargazer.domain.bookmark.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import xyz.ncookie.stargazer.domain.bookmark.entity.BookmarkType;

@Schema(description = "북마크 추가 요청")
public record AddBookmarkRequest(

	@Schema(description = "북마크 타입", example = "SPOT", implementation = BookmarkType.class)
	@NotNull(message = "북마크 타입은 필수입니다.")
	BookmarkType type,

	@Schema(description = "관측지 ID (SPOT 타입일 때 필수, CUSTOM 타입일 때는 null)", example = "1")
	Long spotId,

	@Schema(description = "북마크 이름 (사용자가 지정하는 이름, 최대 50자)", example = "제주도 별보기 명소", maxLength = 50)
	@Size(max = 50, message = "북마크 이름은 최대 50자까지 입력 가능합니다.")
	@NotBlank(message = "북마크 이름은 필수입니다.")
	String name,

	@Schema(description = "위도 (CUSTOM 타입일 때 필수, SPOT 타입일 때는 null)", example = "33.4996")
	Double latitude,

	@Schema(description = "경도 (CUSTOM 타입일 때 필수, SPOT 타입일 때는 null)", example = "126.5312")
	Double longitude,

	@Schema(description = "주소 (CUSTOM 타입일 때 사용, SPOT 타입일 때는 null)", example = "제주특별자치도 제주시 애월읍")
	String address,

	@Schema(description = "메모 (선택, 최대 500자)", example = "밤하늘이 정말 맑아서 별이 잘 보이는 곳입니다.", maxLength = 500)
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
