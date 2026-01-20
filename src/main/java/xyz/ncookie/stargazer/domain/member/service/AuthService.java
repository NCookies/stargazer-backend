package xyz.ncookie.stargazer.domain.member.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.member.dto.response.TokenDto;
import xyz.ncookie.stargazer.domain.member.exception.MemberErrorCode;
import xyz.ncookie.stargazer.domain.member.exception.AuthException;
import xyz.ncookie.stargazer.global.security.jwt.JwtTokenProvider;
import xyz.ncookie.stargazer.global.security.redis.RefreshTokenRedisRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

	private final RefreshTokenRedisRepository refreshTokenRedisRepository;

	private final JwtTokenProvider jwtTokenProvider;

	public TokenDto reissueAccessToken(String refreshToken) {

		if (refreshToken == null) {
			log.debug("토큰 재발급 실패! 유효하지 않은 refresh 토큰입니다.");
			throw new AuthException(MemberErrorCode.INVALID_REFRESH_TOKEN);
		}

		Long memberId = refreshTokenRedisRepository.find(refreshToken);
		if (memberId == null) {
			log.debug("토큰 재발급 실패! 유효하지 않은 refresh 토큰입니다.");
			throw new AuthException(MemberErrorCode.INVALID_REFRESH_TOKEN);
		}

		refreshTokenRedisRepository.delete(refreshToken);

		String newAccessToken = jwtTokenProvider.createAccessToken(memberId);
		String newRefreshToken = jwtTokenProvider.createRefreshToken(memberId);

		refreshTokenRedisRepository.save(newRefreshToken, memberId, JwtTokenProvider.REFRESH_EXPIRE_MS);

		log.debug("토큰 재발급 성공!");
		return new TokenDto(newAccessToken, newRefreshToken);
	}

	public void logout(String refreshToken) {

		if (refreshToken == null) {
			return;
		}

		refreshTokenRedisRepository.delete(refreshToken);

		log.debug("로그아웃 성공!");
	}
}
