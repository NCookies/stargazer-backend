package xyz.ncookie.stargazer.domain.member.exception;

import xyz.ncookie.stargazer.global.exception.BaseException;
import xyz.ncookie.stargazer.global.exception.ErrorCode;

public class AuthException extends BaseException {

	public AuthException(ErrorCode errorCode) {
		super(errorCode);
	}
}
