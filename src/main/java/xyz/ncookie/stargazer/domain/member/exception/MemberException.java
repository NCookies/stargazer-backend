package xyz.ncookie.stargazer.domain.member.exception;

import xyz.ncookie.stargazer.global.exception.BaseException;
import xyz.ncookie.stargazer.global.exception.ErrorCode;

public class MemberException extends BaseException {

	public MemberException(ErrorCode errorCode) {
		super(errorCode);
	}
}
