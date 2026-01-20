package xyz.ncookie.stargazer.domain.member.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다")
	;

	private final HttpStatus status;
	private final String message;
}
