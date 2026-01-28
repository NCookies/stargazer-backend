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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.bookmark.application.BookmarkApplicationService;
import xyz.ncookie.stargazer.domain.bookmark.application.dto.AddBookmarkCommand;
import xyz.ncookie.stargazer.domain.bookmark.application.dto.ModifyBookmarkCommand;
import xyz.ncookie.stargazer.domain.bookmark.dto.request.AddBookmarkRequest;
import xyz.ncookie.stargazer.domain.bookmark.dto.request.ModifyBookmarkRequest;
import xyz.ncookie.stargazer.domain.bookmark.dto.response.BookmarkResponse;
import xyz.ncookie.stargazer.global.security.principal.MemberPrincipal;

@Tag(name = "북마크", description = "북마크 관리 API - 관측지나 사용자 정의 위치를 북마크로 저장하고 관리합니다.")
@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

	private final BookmarkApplicationService bookmarkApplicationService;

	@Operation(
		summary = "북마크 목록 조회",
		description = "현재 로그인한 사용자가 저장한 모든 북마크 목록을 조회합니다. " +
			"SPOT 타입과 CUSTOM 타입 북마크가 모두 포함됩니다. " +
			"JWT 토큰 인증이 필요합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(schema = @Schema(implementation = BookmarkResponse.class))
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증 실패 (토큰이 유효하지 않거나 만료됨)"
		)
	})
	@SecurityRequirement(name = "bearer-jwt")
	@GetMapping
	public List<BookmarkResponse> getBookmarkList(
		@Parameter(description = "인증된 사용자 정보 (JWT에서 자동 추출)", required = true, hidden = true)
		@AuthenticationPrincipal MemberPrincipal principal
	) {

		return bookmarkApplicationService.getBookmarkList(principal.getMemberId());
	}

	@Operation(
		summary = "북마크 추가",
		description = "새로운 북마크를 추가합니다. " +
			"SPOT 타입: 서비스에 등록된 관측지(spotId 필수)를 북마크합니다. " +
			"CUSTOM 타입: 사용자가 직접 입력한 위치(latitude, longitude, address 필수)를 북마크합니다. " +
			"같은 사용자가 같은 명소(SPOT)를 중복 북마크할 수 없습니다. " +
			"JWT 토큰 인증이 필요합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "북마크 추가 성공",
			content = @Content(schema = @Schema(implementation = BookmarkResponse.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 - 유효성 검사 실패 또는 타입별 필수 필드 누락"
		),
		@ApiResponse(
			responseCode = "400",
			description = "이미 해당 명소는 북마크 되어있습니다 (SPOT 타입 중복)"
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증 실패 (토큰이 유효하지 않거나 만료됨)"
		),
		@ApiResponse(
			responseCode = "404",
			description = "존재하지 않는 관측지 ID (SPOT 타입의 spotId가 유효하지 않음)"
		)
	})
	@SecurityRequirement(name = "bearer-jwt")
	@PostMapping
	public BookmarkResponse addBookmark(
		@Parameter(description = "인증된 사용자 정보 (JWT에서 자동 추출)", required = true, hidden = true)
		@AuthenticationPrincipal MemberPrincipal principal,
		@Parameter(description = "북마크 추가 요청 정보", required = true)
		@Valid @RequestBody AddBookmarkRequest request
	) {

		AddBookmarkCommand command = new AddBookmarkCommand(
			principal.getMemberId(),
			request.type(),
			request.spotId(),
			request.name(),
			request.latitude(),
			request.longitude(),
			request.address(),
			request.memo()
		);
		return bookmarkApplicationService.create(command);
	}

	@Operation(
		summary = "북마크 수정",
		description = "기존 북마크의 이름과 메모를 수정합니다. " +
			"본인이 소유한 북마크만 수정할 수 있습니다. " +
			"JWT 토큰 인증이 필요합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "북마크 수정 성공",
			content = @Content(schema = @Schema(implementation = BookmarkResponse.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 - 유효성 검사 실패 (이름 필수, 최대 길이 초과 등)"
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증 실패 (토큰이 유효하지 않거나 만료됨)"
		),
		@ApiResponse(
			responseCode = "401",
			description = "권한 없음 - 본인이 소유하지 않은 북마크는 수정할 수 없습니다"
		),
		@ApiResponse(
			responseCode = "404",
			description = "존재하지 않는 북마크 ID"
		)
	})
	@SecurityRequirement(name = "bearer-jwt")
	@PatchMapping("/{bookmarkId}")
	public BookmarkResponse modifyBookmark(
		@Parameter(description = "인증된 사용자 정보 (JWT에서 자동 추출)", required = true, hidden = true)
		@AuthenticationPrincipal MemberPrincipal principal,
		@Parameter(description = "수정할 북마크 ID", example = "1", required = true)
		@PathVariable Long bookmarkId,
		@Parameter(description = "북마크 수정 요청 정보 (이름, 메모)", required = true)
		@Valid @RequestBody ModifyBookmarkRequest request
	) {

		ModifyBookmarkCommand command = new ModifyBookmarkCommand(
			request.name(),
			request.memo()
		);
		return bookmarkApplicationService.updateName(principal.getMemberId(), bookmarkId, command);
	}

	@Operation(
		summary = "북마크 삭제",
		description = "기존 북마크를 삭제합니다. " +
			"본인이 소유한 북마크만 삭제할 수 있습니다. " +
			"삭제된 북마크는 복구할 수 없습니다. " +
			"JWT 토큰 인증이 필요합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "북마크 삭제 성공"
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증 실패 (토큰이 유효하지 않거나 만료됨)"
		),
		@ApiResponse(
			responseCode = "401",
			description = "권한 없음 - 본인이 소유하지 않은 북마크는 삭제할 수 없습니다"
		),
		@ApiResponse(
			responseCode = "404",
			description = "존재하지 않는 북마크 ID"
		)
	})
	@SecurityRequirement(name = "bearer-jwt")
	@DeleteMapping("/{bookmarkId}")
	public ResponseEntity<Void> removeBookmark(
		@Parameter(description = "인증된 사용자 정보 (JWT에서 자동 추출)", required = true, hidden = true)
		@AuthenticationPrincipal MemberPrincipal principal,
		@Parameter(description = "삭제할 북마크 ID", example = "1", required = true)
		@PathVariable Long bookmarkId
	) {

		bookmarkApplicationService.delete(principal.getMemberId(), bookmarkId);

		return ResponseEntity.ok().build();
	}
}
