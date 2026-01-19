package xyz.ncookie.stargazer.global.exception;

public record ErrorResponse(
	boolean success,
	int status,
	String message
) {
}
