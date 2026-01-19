package xyz.ncookie.stargazer.global.security.jwt;

import jakarta.servlet.http.HttpServletRequest;

public interface RefreshTokenResolver {
	String resolve(HttpServletRequest request);
}
