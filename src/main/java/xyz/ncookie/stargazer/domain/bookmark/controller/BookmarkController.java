package xyz.ncookie.stargazer.domain.bookmark.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.bookmark.dto.request.AddBookmarkRequest;
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
}
