package xyz.ncookie.stargazer.global.security.util;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import xyz.ncookie.stargazer.global.security.jwt.JwtTokenProvider;

@Component
public class CookieUtil {

	public static final String REFRESH_COOKIE_NAME = "refresh_token";

	public ResponseCookie createRefreshTokenCookie(String refreshToken) {
		return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
			.httpOnly(true)
			.secure(true) // HTTPS 환경에서만 전송
			.sameSite("None") // 서로 다른 도메인(프론트/백엔드) 간 쿠키 전송을 위해 None 설정
			.maxAge(JwtTokenProvider.REFRESH_EXPIRE_MS / 1000)
			.path("/")
			.build();
	}

	public ResponseCookie deleteRefreshTokenCookie() {
		return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
			.httpOnly(true)
			.secure(true)
			.path("/")
			.maxAge(0) // 즉시 만료
			.build();
	}
}
