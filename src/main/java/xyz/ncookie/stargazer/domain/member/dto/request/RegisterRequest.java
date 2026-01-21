package xyz.ncookie.stargazer.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
	@Email String email,
	@NotBlank String password,
	@NotBlank String nickname
) {
}
