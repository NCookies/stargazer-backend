package xyz.ncookie.stargazer.domain.member.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.member.dto.request.LoginRequest;
import xyz.ncookie.stargazer.domain.member.dto.request.RegisterRequest;
import xyz.ncookie.stargazer.domain.member.dto.response.AuthTokenResponse;
import xyz.ncookie.stargazer.domain.member.dto.TokenDto;
import xyz.ncookie.stargazer.domain.member.service.AuthService;
import xyz.ncookie.stargazer.global.security.jwt.RefreshToken;
import xyz.ncookie.stargazer.global.security.util.CookieUtil;

@Tag(name = "인증", description = "회원 인증 관련 API (로그인, 회원가입, 토큰 재발급, 로그아웃)")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	private final CookieUtil cookieUtil;

	@Operation(
		summary = "로그인",
		description = "이메일과 비밀번호로 로그인합니다. 성공 시 JWT 액세스 토큰과 리프레시 토큰(쿠키)을 반환합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "로그인 성공",
			content = @Content(schema = @Schema(implementation = AuthTokenResponse.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 (이메일 형식 오류, 필수 필드 누락 등)"
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증 실패 (이메일 또는 비밀번호 불일치)"
		)
	})
	@PostMapping("/login")
	public ResponseEntity<AuthTokenResponse> login(
		@Parameter(description = "로그인 요청 정보", required = true)
		@RequestBody @Valid LoginRequest request
	) {
		TokenDto tokenDto = authService.login(request);
		return tokenResponse(tokenDto);
	}

	@Operation(
		summary = "회원가입",
		description = "새로운 회원을 등록합니다. 성공 시 자동으로 로그인되어 JWT 액세스 토큰과 리프레시 토큰(쿠키)을 반환합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "회원가입 성공",
			content = @Content(schema = @Schema(implementation = AuthTokenResponse.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 (이메일 형식 오류, 필수 필드 누락, 이메일 중복 등)"
		)
	})
	@PostMapping("/register")
	public ResponseEntity<AuthTokenResponse> register(
		@Parameter(description = "회원가입 요청 정보", required = true)
		@RequestBody @Valid RegisterRequest request
	) {
		TokenDto tokenDto = authService.register(request);
		return tokenResponse(tokenDto);
	}

	@Operation(
		summary = "액세스 토큰 재발급",
		description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다. 리프레시 토큰은 쿠키에서 자동으로 읽어옵니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "토큰 재발급 성공",
			content = @Content(schema = @Schema(implementation = AuthTokenResponse.class))
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증 실패 (리프레시 토큰이 유효하지 않거나 만료됨)"
		)
	})
	@PostMapping("/reissue")
	public ResponseEntity<AuthTokenResponse> reissue(
		@Parameter(description = "리프레시 토큰 (쿠키에서 자동 추출)", required = true, hidden = true)
		@RefreshToken String refreshToken
	) {
		TokenDto tokenDto = authService.reissueAccessToken(refreshToken);
		return tokenResponse(tokenDto);
	}

	@Operation(
		summary = "로그아웃",
		description = "현재 사용자를 로그아웃 처리하고 리프레시 토큰을 무효화합니다. 리프레시 토큰은 쿠키에서 자동으로 읽어옵니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "로그아웃 성공"
		),
		@ApiResponse(
			responseCode = "401",
			description = "인증 실패 (리프레시 토큰이 유효하지 않음)"
		)
	})
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
		@Parameter(description = "리프레시 토큰 (쿠키에서 자동 추출)", required = true, hidden = true)
		@RefreshToken String refreshToken
	) {

		authService.logout(refreshToken);

		ResponseCookie deleteCookie = cookieUtil.deleteRefreshTokenCookie();

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
			.build();
	}

	private ResponseEntity<AuthTokenResponse> tokenResponse(TokenDto tokenDto) {
		ResponseCookie rtCookie =
			cookieUtil.createRefreshTokenCookie(tokenDto.refreshToken());

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, rtCookie.toString())
			.body(new AuthTokenResponse(tokenDto.accessToken()));
	}
}
