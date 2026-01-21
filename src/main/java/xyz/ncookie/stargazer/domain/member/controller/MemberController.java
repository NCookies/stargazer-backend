package xyz.ncookie.stargazer.domain.member.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.member.dto.response.MemberInfoResponse;
import xyz.ncookie.stargazer.domain.member.dto.response.MemberValidationResponse;
import xyz.ncookie.stargazer.domain.member.service.MemberService;
import xyz.ncookie.stargazer.global.security.principal.MemberPrincipal;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	@GetMapping("/me")
	public MemberInfoResponse getMyInfo(@AuthenticationPrincipal MemberPrincipal principal) {

		return memberService.getMyInfo(principal.getMemberId());
	}

	@GetMapping("/exists/email")
	public MemberValidationResponse validateEmailDuplicated(@RequestParam(value = "email") String email) {

		return memberService.validateEmailDuplicated(email);
	}
}
