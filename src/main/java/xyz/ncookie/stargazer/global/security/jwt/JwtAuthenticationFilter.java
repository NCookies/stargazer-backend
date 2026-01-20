package xyz.ncookie.stargazer.global.security.jwt;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.member.entity.Role;
import xyz.ncookie.stargazer.global.security.principal.MemberPrincipal;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
		@NonNull FilterChain filterChain) throws ServletException, IOException {

		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			filterChain.doFilter(request, response);
			return;
		}

		String bearerToken = request.getHeader("Authorization");

		if (bearerToken == null || !bearerToken.startsWith(JwtTokenProvider.BEARER_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = jwtTokenProvider.substringToken(bearerToken);

		if (jwtTokenProvider.validate(token)) {

			Claims claims = jwtTokenProvider.extractClaims(token);
			if (claims == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "잘못된 JWT 토큰입니다");
				return;
			}

			Long memberId = Long.parseLong(claims.getSubject());
			Role role = Role.valueOf(claims.get("role", String.class));

			MemberPrincipal principal = new MemberPrincipal(memberId, role);

			Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(auth);
		}

		filterChain.doFilter(request, response);
	}
}
