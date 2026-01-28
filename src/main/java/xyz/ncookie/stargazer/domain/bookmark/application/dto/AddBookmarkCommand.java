package xyz.ncookie.stargazer.domain.bookmark.application.dto;

import xyz.ncookie.stargazer.domain.bookmark.entity.BookmarkType;

/**
 * 북마크 생성 커맨드
 */
public record AddBookmarkCommand(
	Long memberId,
	BookmarkType type,
	Long spotId,
	String name,
	Double latitude,
	Double longitude,
	String address,
	String memo
) {
}
