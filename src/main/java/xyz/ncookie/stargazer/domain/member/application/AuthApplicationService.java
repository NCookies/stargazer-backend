package xyz.ncookie.stargazer.domain.member.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.member.domain.AuthDomainService;
import xyz.ncookie.stargazer.domain.member.domain.MemberDomainService;
import xyz.ncookie.stargazer.domain.member.dto.TokenDto;
import xyz.ncookie.stargazer.domain.member.dto.request.LoginRequest;
import xyz.ncookie.stargazer.domain.member.dto.request.RegisterRequest;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.exception.AuthErrorCode;
import xyz.ncookie.stargazer.domain.member.exception.AuthException;
import xyz.ncookie.stargazer.domain.member.exception.MemberErrorCode;
import xyz.ncookie.stargazer.domain.member.exception.MemberException;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;
import xyz.ncookie.stargazer.global.security.jwt.JwtTokenProvider;
import xyz.ncookie.stargazer.global.security.redis.RefreshTokenRedisRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthApplicationService {

	private final RefreshTokenRedisRepository refreshTokenRedisRepository;
	private final MemberRepository memberRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthDomainService authDomainService;
	private final MemberDomainService memberDomainService;

	@Transactional(readOnly = true)
	public TokenDto reissueAccessToken(String refreshToken) {

		if (refreshToken == null) {
			log.debug("토큰 재발급 실패! 유효하지 않은 refresh 토큰입니다.");
			throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}

		Long memberId = refreshTokenRedisRepository.find(refreshToken);
		if (memberId == null) {
			log.debug("토큰 재발급 실패! 유효하지 않은 refresh 토큰입니다.");
			throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}

		refreshTokenRedisRepository.delete(refreshToken);

		String newAccessToken = jwtTokenProvider.createAccessToken(memberId);
		String newRefreshToken = jwtTokenProvider.createRefreshToken(memberId);

		refreshTokenRedisRepository.save(newRefreshToken, memberId, JwtTokenProvider.REFRESH_EXPIRE_MS);

		log.debug("토큰 재발급 성공!");
		return new TokenDto(newAccessToken, newRefreshToken);
	}

	@Transactional
	public void logout(String refreshToken) {

		if (refreshToken == null) {
			return;
		}

		refreshTokenRedisRepository.delete(refreshToken);
		log.debug("로그아웃 성공!");
	}

	@Transactional
	public TokenDto register(@Valid RegisterRequest request) {

		if (memberDomainService.isEmailDuplicated(request.email())) {
			Member findMember = authDomainService.findByEmail(request.email());
			throw new MemberException(MemberErrorCode.MEMBER_EMAIL_DUPLICATED, findMember.getAuthProvider().getDesc());
		}

		String encodedPassword = authDomainService.encodePassword(request.password());
		Member createdMember = memberRepository.save(
			Member.local(
				request.email(),
				encodedPassword,
				request.nickname()
			)
		);

		String accessToken = jwtTokenProvider.createAccessToken(createdMember.getId());
		String refreshToken = jwtTokenProvider.createRefreshToken(createdMember.getId());

		return new TokenDto(accessToken, refreshToken);
	}

	@Transactional(readOnly = true)
	public TokenDto login(LoginRequest request) {

		Member member = authDomainService.findByEmail(request.email());
		authDomainService.validatePassword(request.password(), member.getPassword());

		String accessToken = jwtTokenProvider.createAccessToken(member.getId());
		String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

		return new TokenDto(accessToken, refreshToken);
	}
}
