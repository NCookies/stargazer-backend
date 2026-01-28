package xyz.ncookie.stargazer.domain.bookmark.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum BookmarkErrorCode implements ErrorCode {

	BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 ID의 북마크 데이터를 찾을 수 없습니다."),
	INVALID_BOOKMARK_TYPE(HttpStatus.BAD_REQUEST, "북마크 타입과 데이터가 일치하지 않습니다."),
	BOOKMARK_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "해당 북마크를 수정 또는 삭제할 권한이 없습니다."),
	;

	private final HttpStatus status;
	private final String message;
}
