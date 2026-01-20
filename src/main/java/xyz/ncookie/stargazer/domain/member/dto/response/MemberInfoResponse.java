package xyz.ncookie.stargazer.domain.member.dto.response;

import xyz.ncookie.stargazer.domain.member.entity.Member;

public record MemberInfoResponse(
	Long memberId,
	String nickname
) {

	public static MemberInfoResponse from(Member member) {

		return new MemberInfoResponse(member.getId(), member.getNickname());
	}
}
