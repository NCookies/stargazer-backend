package xyz.ncookie.stargazer.global.security.jwt;

import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.global.security.util.CookieUtil;

@Component
@RequiredArgsConstructor
public class RefreshTokenArgumentResolver implements HandlerMethodArgumentResolver {

	private static final String AUTHORIZATION_HEADER = "Authorization-Refresh";
	private static final String BEARER_PREFIX = "Bearer ";

	@Override
	public boolean supportsParameter(MethodParameter parameter) {

		return parameter.hasParameterAnnotation(RefreshToken.class)
			&& String.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

		// Web
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (CookieUtil.REFRESH_COOKIE_NAME.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}

		// Mobile App
		String headerToken = request.getHeader(AUTHORIZATION_HEADER);
		if (headerToken != null && headerToken.startsWith(BEARER_PREFIX)) {
			return headerToken.substring(7);
		}

		return null;
	}
}
