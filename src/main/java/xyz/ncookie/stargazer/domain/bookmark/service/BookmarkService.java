package xyz.ncookie.stargazer.domain.bookmark.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.bookmark.dto.request.AddBookmarkRequest;
import xyz.ncookie.stargazer.domain.bookmark.dto.request.ModifyBookmarkRequest;
import xyz.ncookie.stargazer.domain.bookmark.dto.response.BookmarkResponse;
import xyz.ncookie.stargazer.domain.bookmark.entity.Bookmark;
import xyz.ncookie.stargazer.domain.bookmark.entity.BookmarkType;
import xyz.ncookie.stargazer.domain.bookmark.exception.BookmarkErrorCode;
import xyz.ncookie.stargazer.domain.bookmark.exception.BookmarkException;
import xyz.ncookie.stargazer.domain.bookmark.repository.BookmarkRepository;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.service.MemberService;
import xyz.ncookie.stargazer.domain.spot.entity.ObservationSpot;
import xyz.ncookie.stargazer.domain.spot.service.ObservationSpotService;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookmarkService {

	private final MemberService memberService;
	private final ObservationSpotService spotService;

	private final BookmarkRepository bookmarkRepository;

	@Transactional(readOnly = true)
	public List<BookmarkResponse> getBookmarkList(Long memberId) {

		return bookmarkRepository.findAllByMember_Id(memberId)
			.stream()
			.map(BookmarkResponse::from)
			.toList();
	}

	@Transactional
	public BookmarkResponse addBookmark(Long memberId, AddBookmarkRequest request) {

		Member member = memberService.getMemberById(memberId);
		ObservationSpot spot = request.type() == BookmarkType.SPOT
			? spotService.getObservationSpotById(request.spotId())
			: null;

		Bookmark savedBookmark = bookmarkRepository.save(
			Bookmark.builder()
				.member(member)
				.type(request.type())
				.spot(spot)
				.customName(request.name())
				.latitude(request.latitude())
				.longitude(request.longitude())
				.address(request.address())
				.build()
		);

		return BookmarkResponse.from(savedBookmark);
	}

	@Transactional
	public BookmarkResponse modifyBookmark(Long memberId, Long bookmarkId, ModifyBookmarkRequest request) {

		Bookmark bookmark = getBookmarkById(bookmarkId);

		validateBookmarkOwner(memberId, bookmark);

		bookmark.updateBookmarkCustomName(request.name());

		return BookmarkResponse.from(bookmark);
	}

	@Transactional
	public void removeBookmark(Long memberId, Long bookmarkId) {

		Bookmark bookmark = getBookmarkById(bookmarkId);

		validateBookmarkOwner(memberId, bookmark);

		bookmarkRepository.delete(bookmark);
	}

	private Bookmark getBookmarkById(Long bookmarkId) {

		return bookmarkRepository.findById(bookmarkId)
			.orElseThrow(() -> new BookmarkException(BookmarkErrorCode.BOOKMARK_NOT_FOUND, bookmarkId.toString()));
	}

	private void validateBookmarkOwner(Long memberId, Bookmark bookmark) {

		if (!bookmark.isOwner(memberId)) {
			throw new BookmarkException(BookmarkErrorCode.BOOKMARK_UNAUTHORIZED);
		}
	}
}
