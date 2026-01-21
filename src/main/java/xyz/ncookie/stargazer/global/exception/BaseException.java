package xyz.ncookie.stargazer.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {

	private final ErrorCode errorCode;

	protected BaseException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	protected BaseException(ErrorCode errorCode, String detailMessage) {
		super(errorCode.getMessage() + " : " + detailMessage);
		this.errorCode = errorCode;
	}

	protected BaseException(ErrorCode errorCode, Throwable cause) {
		super(errorCode.getMessage(), cause);
		this.errorCode = errorCode;
	}

	public HttpStatus getHttpStatus() {
		return errorCode.getStatus();
	}
}
