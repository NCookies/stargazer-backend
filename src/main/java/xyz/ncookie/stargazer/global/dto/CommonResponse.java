package xyz.ncookie.stargazer.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import xyz.ncookie.stargazer.global.exception.ErrorCode;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommonResponse<T>(
	boolean success,
	int status,
	String message,
	T data
) {

	public static <T> CommonResponse<T> success(boolean success, int status, String message, T data) {
		return CommonResponse.<T>builder()
			.success(success)
			.message(message)
			.status(status)
			.data(data)
			.build();
	}

	public static <T> CommonResponse<T> error(int status, String message, T data) {
		return CommonResponse.<T>builder()
			.success(false)
			.message(message)
			.status(status)
			.data(data)
			.build();
	}

	public static CommonResponse<Void> error(ErrorCode code) {
		return CommonResponse.<Void>builder()
			.success(false)
			.status(code.getStatus().value())
			.message(code.getMessage())
			.build();
	}
}
