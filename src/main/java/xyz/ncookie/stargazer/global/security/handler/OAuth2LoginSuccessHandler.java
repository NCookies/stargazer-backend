package xyz.ncookie.stargazer.global.security.handler;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.global.security.jwt.JwtTokenProvider;
import xyz.ncookie.stargazer.global.security.principal.MemberPrincipal;
import xyz.ncookie.stargazer.global.security.redis.RefreshTokenRedisRepository;
import xyz.ncookie.stargazer.global.security.util.CookieUtil;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final RefreshTokenRedisRepository refreshTokenRedisRepository;

	private final JwtTokenProvider jwtTokenProvider;
	private final CookieUtil cookieUtil;

	@Value("${client.redirect-base-url}")
	private String webClientBaseUrl;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
		throws IOException {

		log.info("OAuth2 로그인 성공!");

		MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();

		Long memberId = principal.getMember().getId();

		String accessToken = jwtTokenProvider.createAccessToken(memberId);
		String refreshToken = jwtTokenProvider.createRefreshToken(memberId);

		refreshTokenRedisRepository.save(refreshToken, memberId, JwtTokenProvider.REFRESH_EXPIRE_MS);


		ResponseCookie rtCookie = cookieUtil.createRefreshTokenCookie(refreshToken);
		response.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());

		// 우선 React 전용
		response.setContentType("application/json");
		response.getWriter().write(
			"{\"accessToken\":\"" + accessToken + "\"}"
		);
		response.sendRedirect(webClientBaseUrl + "/oauth/callback");
	}
}
