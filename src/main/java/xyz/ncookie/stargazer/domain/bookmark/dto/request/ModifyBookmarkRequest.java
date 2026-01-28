package xyz.ncookie.stargazer.domain.bookmark.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "북마크 수정 요청 DTO. 이름과 메모를 수정합니다.")
public record ModifyBookmarkRequest(
	@Schema(description = "수정할 북마크 이름 (최대 50자)", example = "강원도 대관령 (수정)", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 50)
	@Size(max = 50, message = "북마크 이름은 최대 50자까지 입력 가능합니다.")
	@NotBlank(message = "수정할 북마크 이름은 필수입니다.")
	String name,

	@Schema(description = "수정할 메모 (선택, 최대 500자)", example = "겨울에 별 보기 좋음", maxLength = 500)
	@Size(max = 500, message = "메모는 최대 500자까지 입력 가능합니다.")
	String memo
) {
}
