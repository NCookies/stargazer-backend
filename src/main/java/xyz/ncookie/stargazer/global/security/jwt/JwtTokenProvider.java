package xyz.ncookie.stargazer.global.security.jwt;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import xyz.ncookie.stargazer.domain.member.entity.Role;

@Component
public class JwtTokenProvider {

	private final Key key;

	public static final String BEARER_PREFIX = "Bearer ";
	public static final long ACCESS_EXPIRE_MS = 1000L * 60 * 15;     // 15분
	public static final long REFRESH_EXPIRE_MS = 1000L * 60 * 60 * 24 * 14; // 14일

	public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {

		byte[] keyBytes = Base64.getDecoder().decode(secretKey);
		this.key = Keys.hmacShaKeyFor(keyBytes);
	}

	public String createAccessToken(Long memberId) {

		Date now = new Date();

		return Jwts.builder()
			.setSubject(memberId.toString())
			.claim("role", Role.USER)
			.setIssuedAt(now)
			.setExpiration(new Date(now.getTime() + ACCESS_EXPIRE_MS))
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	public String createRefreshToken(Long memberId) {

		Date now = new Date();

		return Jwts.builder()
			.setSubject(memberId.toString())
			.claim("role", Role.USER)
			.setIssuedAt(now)
			.setExpiration(new Date(now.getTime() + REFRESH_EXPIRE_MS))
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	public Claims extractClaims(String token) {

		return Jwts.parserBuilder().setSigningKey(key).build()
			.parseClaimsJws(token)
			.getBody();
	}

	public boolean validate(String token) {

		try {
			Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
			return true;
		} catch (io.jsonwebtoken.JwtException e) {
			return false;
		}
	}

	public String substringToken(String token) {

		if (StringUtils.hasText(token) && token.startsWith(BEARER_PREFIX)) {
			return token.substring(BEARER_PREFIX.length());
		}

		return null;
	}
}
