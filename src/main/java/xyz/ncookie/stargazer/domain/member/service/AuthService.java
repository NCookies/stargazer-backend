package xyz.ncookie.stargazer.domain.member.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.member.exception.MemberErrorCode;
import xyz.ncookie.stargazer.domain.member.exception.MemberException;
import xyz.ncookie.stargazer.global.security.jwt.JwtTokenProvider;
import xyz.ncookie.stargazer.global.security.redis.RefreshTokenRedisRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

	private final JwtTokenProvider jwtTokenProvider;

	private final RefreshTokenRedisRepository refreshTokenRedisRepository;

	public String reissueAccessToken(String refreshToken) {

		Long memberId = jwtTokenProvider.getMemberId(refreshToken);

		String stored = refreshTokenRedisRepository.find(memberId);
		if (!refreshToken.equals(stored)) {
			throw new MemberException(MemberErrorCode.INVALID_REFRESH_TOKEN);
		}

		return jwtTokenProvider.createAccessToken(memberId);
	}

	public void logout(Long memberId) {

		refreshTokenRedisRepository.delete(memberId);
	}
}
