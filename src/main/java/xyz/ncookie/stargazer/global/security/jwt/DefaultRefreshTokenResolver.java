package xyz.ncookie.stargazer.global.security.jwt;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class DefaultRefreshTokenResolver implements RefreshTokenResolver {

	private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	@Override
	public String resolve(HttpServletRequest request) {

		// HttpOnly Cookie (Web)
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}

		// Authorization Header (Mobile)
		String header = request.getHeader(AUTHORIZATION_HEADER);
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			return header.substring(BEARER_PREFIX.length());
		}

		return null;
	}
}
