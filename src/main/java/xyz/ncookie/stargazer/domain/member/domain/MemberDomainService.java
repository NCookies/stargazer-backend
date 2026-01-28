package xyz.ncookie.stargazer.domain.member.domain;

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
public class MemberDomainService {

	private final MemberRepository memberRepository;

	public Member findById(Long memberId) {

		return memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	public boolean isEmailDuplicated(String email) {
		return memberRepository.existsByEmail(email);
	}
}
