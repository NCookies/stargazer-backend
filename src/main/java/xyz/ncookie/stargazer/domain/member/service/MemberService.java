package xyz.ncookie.stargazer.domain.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.member.dto.response.MemberInfoResponse;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.exception.MemberErrorCode;
import xyz.ncookie.stargazer.domain.member.exception.MemberException;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;

	@Transactional(readOnly = true)
	public MemberInfoResponse getMyInfo(Long memberId) {

		Member member = getMemberById(memberId);
		return MemberInfoResponse.from(member);
	}

	public Member getMemberById(Long memberId) {

		return memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
	}
}
