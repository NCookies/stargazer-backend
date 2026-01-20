package xyz.ncookie.stargazer.domain.member.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.member.dto.response.ReissueTokenResponse;
import xyz.ncookie.stargazer.domain.member.dto.response.TokenDto;
import xyz.ncookie.stargazer.domain.member.service.AuthService;
import xyz.ncookie.stargazer.global.security.jwt.RefreshToken;
import xyz.ncookie.stargazer.global.security.util.CookieUtil;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	private final CookieUtil cookieUtil;

	@PostMapping("/reissue")
	public ResponseEntity<ReissueTokenResponse> reissue(@RefreshToken String refreshToken) {

		TokenDto tokenDto = authService.reissueAccessToken(refreshToken);

		ResponseCookie rtCookie = cookieUtil.createRefreshTokenCookie(tokenDto.refreshToken());

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, rtCookie.toString())
			.body(new ReissueTokenResponse(tokenDto.accessToken()));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@RefreshToken String refreshToken) {

		authService.logout(refreshToken);

		ResponseCookie deleteCookie = cookieUtil.deleteRefreshTokenCookie();

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
			.build();
	}
}
