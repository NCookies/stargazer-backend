package xyz.ncookie.stargazer.domain.bookmark.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModifyBookmarkRequest(
	@Size(max = 50, message = "북마크 이름은 최대 50자까지 입력 가능합니다.")
	@NotBlank(message = "수정할 북마크 이름은 필수입니다.")
	String name,

	@Size(max = 500, message = "메모는 최대 500자까지 입력 가능합니다.")
	String memo
) {
}
