package xyz.ncookie.stargazer.domain.member.domain;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.exception.MemberErrorCode;
import xyz.ncookie.stargazer.domain.member.exception.MemberException;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthDomainService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	public Member findByEmail(String email) {

		return memberRepository.findByEmail(email)
			.orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	public void validatePassword(String rawPassword, String encodedPassword) {

		if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
			throw new MemberException(MemberErrorCode.PASSWORD_NOT_MATCH);
		}
	}

	public String encodePassword(String rawPassword) {
		return passwordEncoder.encode(rawPassword);
	}
}
