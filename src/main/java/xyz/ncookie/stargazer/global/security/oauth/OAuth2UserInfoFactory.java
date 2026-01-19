package xyz.ncookie.stargazer.global.security.oauth;

import java.util.Map;

import com.fasterxml.jackson.core.ObjectCodec;

import xyz.ncookie.stargazer.domain.member.entity.AuthProvider;

public class OAuth2UserInfoFactory {

	public static OAuth2UserInfo of(AuthProvider provider, Map<String, Object> attributes) {

		return switch (provider) {
			case KAKAO -> new KakaoOAuth2UserInfo(attributes);
			case GOOGLE -> new GoogleOAuth2UserInfo(attributes);
			case NAVER -> new NaverOAuth2UserInfo(attributes);
			default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
		};
	}
}
