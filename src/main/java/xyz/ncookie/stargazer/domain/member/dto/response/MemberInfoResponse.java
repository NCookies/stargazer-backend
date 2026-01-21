package xyz.ncookie.stargazer.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import xyz.ncookie.stargazer.domain.member.entity.Member;

@Schema(description = "회원 정보 응답 DTO")
public record MemberInfoResponse(
	@Schema(description = "회원 ID", example = "1")
	Long memberId,
	
	@Schema(description = "닉네임", example = "별보러가자")
	String nickname
) {

	public static MemberInfoResponse from(Member member) {

		return new MemberInfoResponse(member.getId(), member.getNickname());
	}
}
