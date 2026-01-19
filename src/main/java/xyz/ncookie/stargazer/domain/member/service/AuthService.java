package xyz.ncookie.stargazer.domain.member.service;

import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.member.exception.MemberErrorCode;
import xyz.ncookie.stargazer.domain.member.exception.MemberException;
import xyz.ncookie.stargazer.global.security.jwt.JwtTokenProvider;
import xyz.ncookie.stargazer.global.security.jwt.RefreshTokenResolver;
import xyz.ncookie.stargazer.global.security.redis.RefreshTokenRedisRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

	private final RefreshTokenRedisRepository refreshTokenRedisRepository;

	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenResolver refreshTokenResolver;

	// TODO: refresh token rotation 적용
	public String reissueAccessToken(String refreshToken) {

		Long memberId = jwtTokenProvider.getMemberId(refreshToken);

		String stored = refreshTokenRedisRepository.find(refreshToken);
		if (stored == null) {
			log.debug("토큰 재발급 실패! 유효하지 않은 refresh 토큰입니다.");
			throw new MemberException(MemberErrorCode.INVALID_REFRESH_TOKEN);
		}

		log.debug("토큰 재발급 성공!");
		return jwtTokenProvider.createAccessToken(memberId);
	}

	public void logout(HttpServletRequest request) {

		String refreshToken = refreshTokenResolver.resolve(request);
		if (refreshToken == null) {
			return;
		}

		log.debug("로그아웃 성공!");
		refreshTokenRedisRepository.delete(refreshToken);
	}
}
