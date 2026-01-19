package xyz.ncookie.stargazer.global.advice;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import xyz.ncookie.stargazer.global.dto.CommonResponse;

@RestControllerAdvice
@RequiredArgsConstructor
public class ResponseBodyWrapper implements ResponseBodyAdvice<Object> {

	private final ObjectMapper objectMapper;

	@Override
	public boolean supports(MethodParameter returnType,
		@NonNull Class<? extends HttpMessageConverter<?>> converterType) {
		Class<?> declaringClass = returnType.getDeclaringClass();
		return declaringClass.isAnnotationPresent(RestController.class);
	}

	@Override
	public Object beforeBodyWrite(
		Object body,
		@NonNull MethodParameter returnType,
		@NonNull MediaType selectedContentType,
		@NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
		@NonNull ServerHttpRequest request,
		@NonNull ServerHttpResponse response
	) {

		String path = request.getURI().getPath();
		if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
			return body;
		}

		// 이미 공통 포맷이라면 스킵 (GlobalExceptionHandler에서 리턴한 경우 등)
		if (body instanceof CommonResponse) {
			return body;
		}

		// 성공 메시지 처리 (어노테이션이 없으면 기본 메시지)
		ResponseMessage rm = returnType.getMethodAnnotation(ResponseMessage.class);
		String message = (rm != null) ? rm.value() : "요청이 성공적으로 처리되었습니다.";
		int status = (response instanceof ServletServerHttpResponse servletResponse)
			? servletResponse.getServletResponse().getStatus()
			: 200;

		// CommonResponse 객체 생성
		CommonResponse<Object> responseBody = CommonResponse.success(true, status, message, body);

		if (body instanceof String) {
			try {
				// String 컨버터가 동작하지 않도록, 미리 JSON 문자열로 바꿔서 리턴
				return objectMapper.writeValueAsString(responseBody);
			} catch (JacksonException e) {
				throw new RuntimeException(e);
			}
		}

		return responseBody;
	}
}
