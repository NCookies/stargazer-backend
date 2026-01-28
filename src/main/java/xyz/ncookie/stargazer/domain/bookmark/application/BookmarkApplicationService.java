package xyz.ncookie.stargazer.domain.bookmark.application;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.bookmark.application.dto.AddBookmarkCommand;
import xyz.ncookie.stargazer.domain.bookmark.application.dto.ModifyBookmarkCommand;
import xyz.ncookie.stargazer.domain.bookmark.domain.BookmarkDomainService;
import xyz.ncookie.stargazer.domain.bookmark.dto.response.BookmarkResponse;
import xyz.ncookie.stargazer.domain.bookmark.entity.Bookmark;
import xyz.ncookie.stargazer.domain.bookmark.entity.BookmarkType;
import xyz.ncookie.stargazer.domain.bookmark.exception.BookmarkErrorCode;
import xyz.ncookie.stargazer.domain.bookmark.exception.BookmarkException;
import xyz.ncookie.stargazer.domain.bookmark.repository.BookmarkRepository;
import xyz.ncookie.stargazer.domain.member.domain.MemberDomainService;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.spot.domain.ObservationSpotDomainService;
import xyz.ncookie.stargazer.domain.spot.entity.ObservationSpot;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookmarkApplicationService {

	private final BookmarkDomainService bookmarkDomainService;
	private final MemberDomainService memberDomainService;
	private final ObservationSpotDomainService observationSpotDomainService;
	private final BookmarkRepository bookmarkRepository;

	@Transactional(readOnly = true)
	public List<BookmarkResponse> getBookmarkList(Long memberId) {

		return bookmarkRepository.findAllByMember_Id(memberId)
			.stream()
			.map(BookmarkResponse::from)
			.toList();
	}

	@Transactional
	public BookmarkResponse create(AddBookmarkCommand command) {

		Member member = memberDomainService.findById(command.memberId());
		ObservationSpot spot = command.type() == BookmarkType.SPOT
			? observationSpotDomainService.findById(command.spotId())
			: null;

		Bookmark newBookmark = Bookmark.builder()
			.member(member)
			.type(command.type())
			.spot(spot)
			.customName(command.name())
			.latitude(command.latitude())
			.longitude(command.longitude())
			.address(command.address())
			.memo(command.memo())
			.build();

		Bookmark savedBookmark;

		try {
			savedBookmark = bookmarkRepository.save(newBookmark);
		} catch (DataIntegrityViolationException e) {
			throw new BookmarkException(BookmarkErrorCode.ALREADY_BOOKMARKED, "spotId=" + command.spotId());
		}

		return BookmarkResponse.from(savedBookmark);
	}

	@Transactional
	public BookmarkResponse updateName(Long memberId, Long bookmarkId, ModifyBookmarkCommand command) {

		Bookmark bookmark = bookmarkDomainService.findById(bookmarkId);
		bookmarkDomainService.validateOwner(memberId, bookmark);
		bookmarkDomainService.updateBookmark(bookmark, command.name(), command.memo());

		return BookmarkResponse.from(bookmark);
	}

	@Transactional
	public void delete(Long memberId, Long bookmarkId) {

		Bookmark bookmark = bookmarkDomainService.findById(bookmarkId);
		bookmarkDomainService.validateOwner(memberId, bookmark);
		bookmarkDomainService.deleteBookmark(bookmark);
	}
}
