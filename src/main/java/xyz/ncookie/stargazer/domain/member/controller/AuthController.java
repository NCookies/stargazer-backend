package xyz.ncookie.stargazer.domain.member.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.member.dto.request.LoginRequest;
import xyz.ncookie.stargazer.domain.member.dto.request.RegisterRequest;
import xyz.ncookie.stargazer.domain.member.dto.response.AuthTokenResponse;
import xyz.ncookie.stargazer.domain.member.dto.TokenDto;
import xyz.ncookie.stargazer.domain.member.service.AuthService;
import xyz.ncookie.stargazer.global.security.jwt.RefreshToken;
import xyz.ncookie.stargazer.global.security.util.CookieUtil;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	private final CookieUtil cookieUtil;

	@PostMapping("/login")
	public ResponseEntity<AuthTokenResponse> login(@RequestBody @Valid LoginRequest request) {
		TokenDto tokenDto = authService.login(request);
		return tokenResponse(tokenDto);
	}

	@PostMapping("/register")
	public ResponseEntity<AuthTokenResponse> register(@RequestBody @Valid RegisterRequest request) {
		TokenDto tokenDto = authService.register(request);
		return tokenResponse(tokenDto);
	}

	@PostMapping("/reissue")
	public ResponseEntity<AuthTokenResponse> reissue(@RefreshToken String refreshToken) {
		TokenDto tokenDto = authService.reissueAccessToken(refreshToken);
		return tokenResponse(tokenDto);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@RefreshToken String refreshToken) {

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
