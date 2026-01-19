package xyz.ncookie.stargazer.domain.member.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.global.security.jwt.JwtTokenProvider;
import xyz.ncookie.stargazer.global.security.redis.RefreshTokenRedisRepository;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenRedisRepository refreshTokenRedisRepository;

	@PostMapping("/reissue")
	public ResponseEntity<?> reissue(@CookieValue String refreshToken) {

		Long memberId = jwtTokenProvider.getMemberId(refreshToken);

		String stored = refreshTokenRedisRepository.find(memberId);
		if (!refreshToken.equals(stored)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		String newAccessToken = jwtTokenProvider.createAccessToken(memberId);

		return ResponseEntity.ok(
			Map.of("accessToken", newAccessToken)
		);
	}
}
