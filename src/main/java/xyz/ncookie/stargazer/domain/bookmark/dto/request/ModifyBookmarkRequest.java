package xyz.ncookie.stargazer.domain.bookmark.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ModifyBookmarkRequest(
	@NotBlank(message = "수정할 북마크 이름은 필수입니다.")
	String name
) {
}
