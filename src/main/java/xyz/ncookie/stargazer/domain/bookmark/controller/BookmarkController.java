package xyz.ncookie.stargazer.domain.bookmark.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.bookmark.dto.request.AddBookmarkRequest;
import xyz.ncookie.stargazer.domain.bookmark.dto.request.ModifyBookmarkRequest;
import xyz.ncookie.stargazer.domain.bookmark.dto.response.BookmarkResponse;
import xyz.ncookie.stargazer.domain.bookmark.service.BookmarkService;
import xyz.ncookie.stargazer.global.security.principal.MemberPrincipal;

@Tag(name = "북마크", description = "북마크 목록 조회, 추가, 수정, 삭제 API. 모든 엔드포인트는 JWT 인증이 필요합니다.")
@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

	private final BookmarkService bookmarkService;

	@Operation(
		summary = "북마크 목록 조회",
		description = "현재 로그인한 사용자의 북마크 목록을 조회합니다. SPOT(서비스 명소) 타입과 CUSTOM(사용자 정의 장소) 타입을 모두 반환합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(
				mediaType = "application/json",
				array = @ArraySchema(schema = @Schema(implementation = BookmarkResponse.class))
			)
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증 실패 (JWT 토큰이 유효하지 않거나 만료됨)"
		)
	})
	@SecurityRequirement(name = "bearer-jwt")
	@GetMapping
	public List<BookmarkResponse> getBookmarkList(
		@Parameter(description = "인증된 사용자 정보 (JWT에서 자동 추출)", required = true, hidden = true)
		@AuthenticationPrincipal MemberPrincipal principal
	) {

		return bookmarkService.getBookmarkList(principal.getMemberId());
	}

	@Operation(
		summary = "북마크 추가",
		description = "새 북마크를 등록합니다. 타입별 요구사항: SPOT — spotId 필수; CUSTOM — latitude, longitude 필수, address 권장. " +
			"동일 멤버가 같은 명소(SPOT)를 중복 북마크할 수 없습니다 (400 ALREADY_BOOKMARKED)."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "추가 성공",
			content = @Content(schema = @Schema(implementation = BookmarkResponse.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 — 유효성 검사 실패(필수 필드 누락, name 50자 초과, memo 500자 초과), " +
				"SPOT 타입인데 spotId 누락, CUSTOM 타입인데 좌표 누락, 이미 해당 명소 북마크됨(ALREADY_BOOKMARKED)"
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증 실패 (JWT 토큰이 유효하지 않거나 만료됨)"
		),
		@ApiResponse(
			responseCode = "404",
			description = "SPOT 타입인데 해당 spotId의 관측지를 찾을 수 없음"
		)
	})
	@SecurityRequirement(name = "bearer-jwt")
	@PostMapping
	public BookmarkResponse addBookmark(
		@Parameter(description = "인증된 사용자 정보 (JWT에서 자동 추출)", required = true, hidden = true)
		@AuthenticationPrincipal MemberPrincipal principal,
		@Parameter(description = "북마크 추가 요청 (타입, spotId/좌표, 이름, 메모 등)", required = true)
		@Valid @RequestBody AddBookmarkRequest request
	) {

		return bookmarkService.addBookmark(principal.getMemberId(), request);
	}

	@Operation(
		summary = "북마크 수정",
		description = "기존 북마크의 이름(name)과 메모(memo)를 수정합니다. 본인 소유의 북마크만 수정할 수 있습니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "수정 성공",
			content = @Content(schema = @Schema(implementation = BookmarkResponse.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 — 유효성 검사 실패(name 누락, name 50자 초과, memo 500자 초과)"
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증 실패 또는 해당 북마크에 대한 수정 권한 없음 (BOOKMARK_UNAUTHORIZED)"
		),
		@ApiResponse(
			responseCode = "404",
			description = "해당 ID의 북마크를 찾을 수 없음 (BOOKMARK_NOT_FOUND)"
		)
	})
	@SecurityRequirement(name = "bearer-jwt")
	@PatchMapping("/{bookmarkId}")
	public BookmarkResponse modifyBookmark(
		@Parameter(description = "인증된 사용자 정보 (JWT에서 자동 추출)", required = true, hidden = true)
		@AuthenticationPrincipal MemberPrincipal principal,
		@Parameter(description = "수정할 북마크 ID", required = true, example = "1")
		@PathVariable Long bookmarkId,
		@Parameter(description = "수정할 이름·메모", required = true)
		@Valid @RequestBody ModifyBookmarkRequest request
	) {

		return bookmarkService.modifyBookmark(principal.getMemberId(), bookmarkId, request);
	}

	@Operation(
		summary = "북마크 삭제",
		description = "북마크를 삭제합니다. 본인 소유의 북마크만 삭제할 수 있으며, DB에서 완전히 제거됩니다(hard delete)."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "삭제 성공 (응답 본문 없음)"
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증 실패 또는 해당 북마크에 대한 삭제 권한 없음 (BOOKMARK_UNAUTHORIZED)"
		),
		@ApiResponse(
			responseCode = "404",
			description = "해당 ID의 북마크를 찾을 수 없음 (BOOKMARK_NOT_FOUND)"
		)
	})
	@SecurityRequirement(name = "bearer-jwt")
	@DeleteMapping("/{bookmarkId}")
	public ResponseEntity<Void> removeBookmark(
		@Parameter(description = "인증된 사용자 정보 (JWT에서 자동 추출)", required = true, hidden = true)
		@AuthenticationPrincipal MemberPrincipal principal,
		@Parameter(description = "삭제할 북마크 ID", required = true, example = "1")
		@PathVariable Long bookmarkId
	) {

		bookmarkService.removeBookmark(principal.getMemberId(), bookmarkId);

		return ResponseEntity.ok().build();
	}
}
