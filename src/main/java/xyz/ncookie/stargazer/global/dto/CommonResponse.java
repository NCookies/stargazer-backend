package xyz.ncookie.stargazer.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommonResponse<T>(
	boolean success,
	int status,
	String message,
	T data
) {

	public static <T> CommonResponse<T> of(boolean success, int status, String message, T data) {
		return CommonResponse.<T>builder()
			.success(success)
			.message(message)
			.status(status)
			.data(data)
			.build();
	}
}
