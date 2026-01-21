package xyz.ncookie.stargazer.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 중복 검증 응답 DTO")
public record MemberValidationResponse(
	@Schema(description = "검증 결과 (true: 사용 가능, false: 중복됨)", example = "true")
	boolean validated
) {
}
