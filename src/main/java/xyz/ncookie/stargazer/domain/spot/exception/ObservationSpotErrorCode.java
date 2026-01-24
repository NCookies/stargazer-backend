package xyz.ncookie.stargazer.domain.spot.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum ObservationSpotErrorCode implements ErrorCode {

	SPOT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 ID의 명소 데이터를 찾을 수 없습니다.")
	;

	private final HttpStatus status;
	private final String message;
}
