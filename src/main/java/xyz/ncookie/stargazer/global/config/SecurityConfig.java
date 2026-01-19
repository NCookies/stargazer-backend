package xyz.ncookie.stargazer.global.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.global.security.jwt.JwtAuthenticationFilter;
import xyz.ncookie.stargazer.global.security.jwt.JwtTokenProvider;
import xyz.ncookie.stargazer.global.security.handler.OAuth2LoginFailureHandler;
import xyz.ncookie.stargazer.global.security.service.CustomOAuth2UserService;
import xyz.ncookie.stargazer.global.security.handler.OAuth2LoginSuccessHandler;
import xyz.ncookie.stargazer.global.security.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final CustomUserDetailsService userDetailsService;
	private final CustomOAuth2UserService customOAuth2UserService;

	private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
	private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

	private final JwtTokenProvider jwtTokenProvider;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) {

		http
			.csrf(AbstractHttpConfigurer::disable) 		// REST API이므로 CSRF 보안 필요 없음
			.formLogin(AbstractHttpConfigurer::disable) // 폼 로그인 안 씀
			.httpBasic(AbstractHttpConfigurer::disable) // HTTP Basic 안 씀
			.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)) // H2 콘솔 허용
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))

			// 세션을 사용하지 않음 (Stateless)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

			// URL별 권한 관리
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()			// preflight 무조건 허용
				.requestMatchers("/", "/css/**", "/images/**", "/js/**", "/favicon.ico", "/h2-console/**").permitAll()

				.requestMatchers("/api/v1/spots/**", "/api/v1/analyze", "/api/v1/forecast").permitAll() 	// 대부분의 기능은 인증 없이 사용 가능
				.requestMatchers("/api/v1/auth/**", "/login/**", "/oauth2/**").permitAll()

				.anyRequest().authenticated() // 나머지는 인증 필요
			)

			.userDetailsService(userDetailsService)

			// 소셜 로그인 설정
			.oauth2Login(oauth2 -> oauth2
					.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
					.successHandler(oAuth2LoginSuccessHandler)
					.failureHandler(oAuth2LoginFailureHandler)
			);

		http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {

		CorsConfiguration config = new CorsConfiguration();

		config.setAllowedOrigins(List.of("http://localhost:3000"));
		config.setAllowedOrigins(List.of("https://www.byeolbolil.xyz/"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		config.setAllowCredentials(true);

		// 프론트에서 Authorization 헤더 읽어야 한다면
		config.setExposedHeaders(List.of("Authorization"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
