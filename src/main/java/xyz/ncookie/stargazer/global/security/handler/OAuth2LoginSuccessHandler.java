package xyz.ncookie.stargazer.global.security.handler;

import java.io.IOException;

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

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenRedisRepository refreshTokenRedisRepository;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
		throws IOException {

		log.info("OAuth2 로그인 성공!");

		MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();

		Long memberId = principal.getMember().getId();

		String accessToken = jwtTokenProvider.createAccessToken(memberId);
		String refreshToken = jwtTokenProvider.createRefreshToken(memberId);

		refreshTokenRedisRepository.save(memberId, refreshToken, JwtTokenProvider.REFRESH_EXPIRE_MS);

		ResponseCookie rtCookie = ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(true)
			.secure(true)
			.sameSite("None")
			.path("/")
			.maxAge(JwtTokenProvider.REFRESH_EXPIRE_MS / 1000)
			.build();

		response.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());

		response.sendRedirect("http://localhost:3000/oauth/callback");
		// response.setContentType("application/json");
		// response.getWriter().write(
		// 	"{\"accessToken\":\"" + accessToken + "\"}"
		// );
	}
}
