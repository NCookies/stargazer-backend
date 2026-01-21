package xyz.ncookie.stargazer.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "회원가입 요청 DTO")
public record RegisterRequest(
	@Schema(description = "이메일 주소", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
	@Email String email,
	
	@Schema(
		description = "비밀번호 (8자 이상, 영문자/숫자/특수문자(!@#$%^&*) 각각 최소 1개 이상 포함)",
		example = "password123!",
		requiredMode = Schema.RequiredMode.REQUIRED,
		pattern = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$"
	)
	@Pattern(
		regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$",
		message = "비밀번호는 8자 이상, 문자/숫자/특수문자를 포함해야 합니다."
	)
	@NotBlank String password,
	
	@Schema(description = "닉네임", example = "별보러가자", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank String nickname
) {
}
