package xyz.ncookie.stargazer.domain.bookmark.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum BookmarkErrorCode implements ErrorCode {

	INVALID_BOOKMARK_TYPE(HttpStatus.BAD_REQUEST, "북마크 타입과 데이터가 일치하지 않습니다.")
	;

	private final HttpStatus status;
	private final String message;
}
