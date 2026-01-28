package xyz.ncookie.stargazer.domain.bookmark.domain;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.bookmark.entity.Bookmark;
import xyz.ncookie.stargazer.domain.bookmark.exception.BookmarkErrorCode;
import xyz.ncookie.stargazer.domain.bookmark.exception.BookmarkException;
import xyz.ncookie.stargazer.domain.bookmark.repository.BookmarkRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookmarkDomainService {

	private final BookmarkRepository bookmarkRepository;

	public Bookmark findById(Long bookmarkId) {

		return bookmarkRepository.findById(bookmarkId)
			.orElseThrow(() -> new BookmarkException(BookmarkErrorCode.BOOKMARK_NOT_FOUND, bookmarkId.toString()));
	}

	public void validateOwner(Long memberId, Bookmark bookmark) {

		if (!bookmark.isOwner(memberId)) {
			throw new BookmarkException(BookmarkErrorCode.BOOKMARK_UNAUTHORIZED);
		}
	}

	public void updateBookmark(Bookmark bookmark, String name, String memo) {
		bookmark.updateBookmark(name, memo);
	}

	public void deleteBookmark(Bookmark bookmark) {
		bookmarkRepository.delete(bookmark);
	}
}
