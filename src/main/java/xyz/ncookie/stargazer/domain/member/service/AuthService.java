package xyz.ncookie.stargazer.domain.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class AuthService {

	private final RefreshTokenRedisRepository refreshTokenRedisRepository;
	private final MemberRepository memberRepository;

	private final JwtTokenProvider jwtTokenProvider;
	private final PasswordEncoder passwordEncoder;

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

	public void logout(String refreshToken) {

		if (refreshToken == null) {
			return;
		}

		refreshTokenRedisRepository.delete(refreshToken);

		log.debug("로그아웃 성공!");
	}

	public TokenDto register(@Valid RegisterRequest request) {

		// 이메일 중복 검사
		if (memberRepository.existsByEmail(request.email())) {
			// 우선은 같은 명의의 회원이라도 별개로 인식하도록 정책 설정
			Member findMember = getMemberByEmail(request.email());
			throw new MemberException(MemberErrorCode.MEMBER_EMAIL_DUPLICATED, findMember.getAuthProvider().getDesc());
		}

		String encodedPassword = passwordEncoder.encode(request.password());
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

	public TokenDto login(LoginRequest request) {

		Member member = getMemberByEmail(request.email());

		if (!passwordEncoder.matches(request.password(), member.getPassword())) {
			throw new MemberException(MemberErrorCode.PASSWORD_NOT_MATCH);
		}

		String accessToken = jwtTokenProvider.createAccessToken(member.getId());
		String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

		return new TokenDto(accessToken, refreshToken);
	}

	private Member getMemberByEmail(String email) {

		return memberRepository.findByEmail(email)
			.orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
	}
}
