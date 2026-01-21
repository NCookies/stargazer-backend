package xyz.ncookie.stargazer.global.security.jwt;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;
import xyz.ncookie.stargazer.global.dto.CommonResponse;
import xyz.ncookie.stargazer.global.exception.ErrorCode;
import xyz.ncookie.stargazer.global.security.exception.JwtAuthenticationErrorCode;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) throws IOException {

		ErrorCode code = JwtAuthenticationErrorCode.UNAUTHORIZED;

		response.setStatus(code.getStatus().value());
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		CommonResponse<Void> body = CommonResponse.error(code);

		objectMapper.writeValue(response.getWriter(), body);
	}
}
