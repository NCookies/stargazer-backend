package xyz.ncookie.stargazer.domain.bookmark.application.dto;

import xyz.ncookie.stargazer.domain.bookmark.dto.request.AddBookmarkRequest;
import xyz.ncookie.stargazer.domain.bookmark.entity.BookmarkType;

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

	public static AddBookmarkCommand from(Long memberId, AddBookmarkRequest request) {
		return new AddBookmarkCommand(
			memberId,
			request.type(),
			request.spotId(),
			request.name(),
			request.latitude(),
			request.longitude(),
			request.address(),
			request.memo()
		);
	}
}
