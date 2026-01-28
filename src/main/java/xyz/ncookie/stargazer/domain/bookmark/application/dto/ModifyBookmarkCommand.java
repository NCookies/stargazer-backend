package xyz.ncookie.stargazer.domain.bookmark.application.dto;

/**
 * 북마크 수정 커맨드
 */
public record ModifyBookmarkCommand(
	String name,
	String memo
) {
}
