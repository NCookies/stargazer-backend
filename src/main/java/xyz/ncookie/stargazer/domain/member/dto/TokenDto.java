package xyz.ncookie.stargazer.domain.member.dto;

public record TokenDto(
	String accessToken,
	String refreshToken
) {
}
