package xyz.ncookie.stargazer.domain.member.dto.response;

public record TokenDto(
	String accessToken,
	String refreshToken
) {
}
