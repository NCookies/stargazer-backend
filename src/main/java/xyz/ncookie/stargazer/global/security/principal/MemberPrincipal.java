package xyz.ncookie.stargazer.global.security.principal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.member.entity.Role;

/**
 * 자체 로그인과 OAuth 로그인 모두 동일한 Principal을 사용
 */
@Getter
@RequiredArgsConstructor
public class MemberPrincipal implements UserDetails, OAuth2User {

	private final Long memberId;
	private final Role role;
	private final Map<String, Object> attributes;

	public MemberPrincipal(Long memberId, Role role) {
		this.memberId = memberId;
		this.role = role;
		this.attributes = Collections.emptyMap();
	}

	// OAuth2
	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	// 공통
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(role.getKey()));
	}

	@Override
	public @Nullable String getPassword() {
		return "";
	}

	@Override
	public String getUsername() {
		return String.valueOf(memberId);
	}

	@Override
	public String getName() {
		return String.valueOf(memberId);
	}

	// 기타는 true 처리
	@Override public boolean isAccountNonExpired() { return true; }
	@Override public boolean isAccountNonLocked() { return true; }
	@Override public boolean isCredentialsNonExpired() { return true; }
	@Override public boolean isEnabled() { return true; }
}
