package xyz.ncookie.stargazer.domain.bookmark.application.dto;

import xyz.ncookie.stargazer.domain.bookmark.dto.request.ModifyBookmarkRequest;

public record ModifyBookmarkCommand(
	String name,
	String memo
) {

	public static ModifyBookmarkCommand from(ModifyBookmarkRequest request) {
		return new ModifyBookmarkCommand(
			request.name(),
			request.memo()
		);
	}
}
