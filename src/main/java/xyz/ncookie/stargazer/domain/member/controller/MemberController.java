package xyz.ncookie.stargazer.domain.member.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
import xyz.ncookie.stargazer.domain.member.application.MemberApplicationService;
import xyz.ncookie.stargazer.domain.member.dto.response.MemberInfoResponse;
import xyz.ncookie.stargazer.domain.member.dto.response.MemberValidationResponse;
import xyz.ncookie.stargazer.global.security.principal.MemberPrincipal;

@Tag(name = "회원", description = "회원 정보 관련 API")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberApplicationService memberApplicationService;

	@Operation(
		summary = "내 정보 조회",
		description = "현재 로그인한 사용자의 정보를 조회합니다. JWT 토큰 인증이 필요합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(schema = @Schema(implementation = MemberInfoResponse.class))
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증 실패 (토큰이 유효하지 않거나 만료됨)"
		)
	})
	@SecurityRequirement(name = "bearer-jwt")
	@GetMapping("/me")
	public MemberInfoResponse getMyInfo(
		@Parameter(description = "인증된 사용자 정보 (JWT에서 자동 추출)", required = true, hidden = true)
		@AuthenticationPrincipal MemberPrincipal principal
	) {

		return memberApplicationService.getMyInfo(principal.getMemberId());
	}

	@Operation(
		summary = "이메일 중복 검증",
		description = "회원가입 시 이메일 중복 여부를 확인합니다. 인증 없이 사용 가능합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "검증 완료",
			content = @Content(schema = @Schema(implementation = MemberValidationResponse.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 (이메일 형식 오류 등)"
		)
	})
	@GetMapping("/exists/email")
	public MemberValidationResponse validateEmailDuplicated(
		@Parameter(description = "검증할 이메일 주소", example = "user@example.com", required = true)
		@RequestParam(value = "email") String email
	) {

		return memberApplicationService.validateEmailDuplicated(email);
	}
}
