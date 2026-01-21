package xyz.ncookie.stargazer.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청 DTO")
public record LoginRequest(
	@Schema(description = "이메일 주소", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
	@Email String email,
	
	@Schema(description = "비밀번호", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank String password
) {
}
