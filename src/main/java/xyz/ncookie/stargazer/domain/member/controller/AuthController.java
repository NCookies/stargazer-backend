package xyz.ncookie.stargazer.domain.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.member.dto.response.ReissueTokenResponse;
import xyz.ncookie.stargazer.domain.member.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/reissue")
	public ReissueTokenResponse reissue(@CookieValue String refreshToken) {

		String newAccessToken = authService.reissueAccessToken(refreshToken);
		return new ReissueTokenResponse(newAccessToken);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {

		authService.logout(request);
		return ResponseEntity.ok().build();
	}
}
