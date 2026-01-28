package xyz.ncookie.stargazer.domain.member.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.member.domain.MemberDomainService;
import xyz.ncookie.stargazer.domain.member.dto.response.MemberInfoResponse;
import xyz.ncookie.stargazer.domain.member.dto.response.MemberValidationResponse;
import xyz.ncookie.stargazer.domain.member.entity.Member;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberApplicationService {

	private final MemberDomainService memberDomainService;

	@Transactional(readOnly = true)
	public MemberInfoResponse getMyInfo(Long memberId) {

		Member member = memberDomainService.findById(memberId);
		return MemberInfoResponse.from(member);
	}

	@Transactional(readOnly = true)
	public MemberValidationResponse validateEmailDuplicated(String email) {

		boolean isDuplicated = memberDomainService.isEmailDuplicated(email);
		return new MemberValidationResponse(!isDuplicated);
	}
}
