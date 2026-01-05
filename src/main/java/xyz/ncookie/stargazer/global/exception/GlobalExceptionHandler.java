package xyz.ncookie.stargazer.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.global.dto.CommonResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<CommonResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {

		log.debug("잘못된 요청 인자: ", e);

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(CommonResponse.of(false, HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<CommonResponse<Void>> handleGeneralException(Exception e) {

		log.error("예상하지 못한 예외 발생: ", e);

		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(CommonResponse.of(false, HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), null));
	}
}
