package xyz.ncookie.stargazer.domain.bookmark.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import xyz.ncookie.stargazer.domain.bookmark.entity.BookmarkType;

@Schema(description = "북마크 추가 요청 DTO. SPOT 타입 시 spotId 필수, CUSTOM 타입 시 latitude·longitude 필수.")
public record AddBookmarkRequest(

	@Schema(description = "북마크 타입", example = "SPOT", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"SPOT", "CUSTOM"})
	@NotNull(message = "북마크 타입은 필수입니다.")
	BookmarkType type,

	@Schema(description = "관측지(명소) ID. SPOT 타입일 때 필수.", example = "1")
	Long spotId,

	@Schema(description = "북마크 이름 (최대 50자)", example = "강원도 대관령", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 50)
	@Size(max = 50, message = "북마크 이름은 최대 50자까지 입력 가능합니다.")
	@NotBlank(message = "북마크 이름은 필수입니다.")
	String name,

	@Schema(description = "위도. CUSTOM 타입일 때 필수.", example = "37.5665")
	Double latitude,

	@Schema(description = "경도. CUSTOM 타입일 때 필수.", example = "126.9780")
	Double longitude,

	@Schema(description = "주소. CUSTOM 타입일 때 사용.", example = "강원도 평창군 대관령면")
	String address,

	@Schema(description = "메모 (선택, 최대 500자)", example = "겨울에 별 보기 좋음", maxLength = 500)
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
