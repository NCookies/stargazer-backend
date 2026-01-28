package xyz.ncookie.stargazer.domain.bookmark.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.bookmark.dto.request.AddBookmarkRequest;
import xyz.ncookie.stargazer.domain.bookmark.dto.request.ModifyBookmarkRequest;
import xyz.ncookie.stargazer.domain.bookmark.dto.response.BookmarkResponse;
import xyz.ncookie.stargazer.domain.bookmark.service.BookmarkService;
import xyz.ncookie.stargazer.global.security.principal.MemberPrincipal;

@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

	private final BookmarkService bookmarkService;

	@GetMapping
	public List<BookmarkResponse> getBookmarkList(@AuthenticationPrincipal MemberPrincipal principal) {

		return bookmarkService.getBookmarkList(principal.getMemberId());
	}

	@PostMapping
	public BookmarkResponse addBookmark(
		@AuthenticationPrincipal MemberPrincipal principal,
		@Valid @RequestBody AddBookmarkRequest request
	) {

		return bookmarkService.addBookmark(principal.getMemberId(), request);
	}

	@PutMapping("/{bookmarkId}")
	public BookmarkResponse modifyBookmark(
		@AuthenticationPrincipal MemberPrincipal principal,
		@PathVariable Long bookmarkId,
		@Valid @RequestBody ModifyBookmarkRequest request
	) {

		return bookmarkService.modifyBookmark(principal.getMemberId(), bookmarkId, request);
	}

	@DeleteMapping("/{bookmarkId}")
	public ResponseEntity<Void> removeBookmark(
		@AuthenticationPrincipal MemberPrincipal principal,
		@PathVariable Long bookmarkId
	) {

		bookmarkService.removeBookmark(principal.getMemberId(), bookmarkId);

		return ResponseEntity.ok().build();
	}
}
