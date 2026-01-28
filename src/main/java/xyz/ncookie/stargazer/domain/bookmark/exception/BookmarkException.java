package xyz.ncookie.stargazer.domain.bookmark.exception;

import xyz.ncookie.stargazer.global.exception.BaseException;
import xyz.ncookie.stargazer.global.exception.ErrorCode;

public class BookmarkException extends BaseException {

	public BookmarkException(ErrorCode errorCode) {
		super(errorCode);
	}

	public BookmarkException(ErrorCode errorCode, String detailMessage) {
		super(errorCode, detailMessage);
	}
}
