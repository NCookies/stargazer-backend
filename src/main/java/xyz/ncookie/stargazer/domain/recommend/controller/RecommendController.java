package xyz.ncookie.stargazer.domain.recommend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.recommend.application.RecommendApplicationService;
import xyz.ncookie.stargazer.domain.recommend.dto.response.RecommendedBookmarkResponse;
import xyz.ncookie.stargazer.global.security.principal.MemberPrincipal;

@Tag(name = "추천", description = "관측 추천 API - 오늘 관측이 적합한 북마크 장소를 추천합니다.")
@RestController
@RequestMapping("/api/v1/recommends")
@RequiredArgsConstructor
public class RecommendController {

	private final RecommendApplicationService recommendApplicationService;

	@Operation(
		summary = "오늘 관측 추천 북마크 조회",
		description = "사용자의 북마크 목록 중 오늘 저녁부터 내일 일출 전까지의 관측 적합도를 분석하여 " +
			"상위 5개를 추천합니다. 각 북마크의 날씨 예보를 조회하여 관측 점수를 계산합니다. " +
			"JWT 토큰 인증이 필요합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(schema = @Schema(implementation = RecommendedBookmarkResponse.class))
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증 실패 (토큰이 유효하지 않거나 만료됨)"
		)
	})
	@SecurityRequirement(name = "bearer-jwt")
	@GetMapping("/bookmarks/today")
	public ResponseEntity<List<RecommendedBookmarkResponse>> getTodayRecommendedBookmarks(
		@Parameter(description = "인증된 사용자 정보 (JWT에서 자동 추출)", required = true, hidden = true)
		@AuthenticationPrincipal MemberPrincipal principal
	) {

		List<RecommendedBookmarkResponse> recommendations = 
			recommendApplicationService.getTodayRecommendedBookmarks(principal.getMemberId());

		return ResponseEntity.ok(recommendations);
	}
}
