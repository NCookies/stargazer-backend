package xyz.ncookie.stargazer.domain.spot.exception;

import xyz.ncookie.stargazer.global.exception.BaseException;
import xyz.ncookie.stargazer.global.exception.ErrorCode;

public class ObservationSpotException extends BaseException {

	public ObservationSpotException(ErrorCode errorCode) {
		super(errorCode);
	}

	public ObservationSpotException(ErrorCode errorCode, String detailMessage) {
		super(errorCode, detailMessage);
	}
}
