package xyz.ncookie.stargazer.global.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.global.dto.CommonResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BaseException.class)
	public ResponseEntity<CommonResponse<Void>> handleBaseException(BaseException e) {

		ErrorCode code = e.getErrorCode();

		return ResponseEntity
			.status(code.getStatus())
			.body(CommonResponse.error(code));
	}

	/**
	 * [GET] @ModelAttribute 유효성 검사 실패 시 발생 (BindException)
	 * [POST] @RequestBody 유효성 검사 실패 시 발생 (MethodArgumentNotValidException)
	 * 두 예외를 모두 잡아서 처리
	 * MethodArgumentNotValidException은 BindException을 상속받으므로 BindException으로 한 번에 처리가 가능
	 */
	@ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
	public ResponseEntity<CommonResponse<Map<String, String>>> handleValidationException(BindException e) {

		BindingResult bindingResult = e.getBindingResult();
		Map<String, String> errorMap = new HashMap<>();

		for (FieldError fieldError : bindingResult.getFieldErrors()) {
			String message;

			// 변환(TypeMismatch) 실패인지 체크
			if (fieldError.isBindingFailure()) {
				message = "입력 형식이 올바르지 않습니다."; // "Failed to convert..." 대신 나갈 메시지
			} else {
				message = fieldError.getDefaultMessage(); // @Annotation 메시지
			}

			errorMap.put(fieldError.getField(), message);
		}

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(CommonResponse.error(HttpStatus.BAD_REQUEST.value(), "유효성 검사에 실패하였습니다.", errorMap));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<CommonResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {

		log.debug("잘못된 요청 인자: ", e);

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(CommonResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<CommonResponse<Void>> handleGeneralException(Exception e) {

		log.error("예상하지 못한 예외 발생: ", e);

		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(CommonResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), null));
	}
}
