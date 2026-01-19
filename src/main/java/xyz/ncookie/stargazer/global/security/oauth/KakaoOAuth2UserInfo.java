package xyz.ncookie.stargazer.global.security.oauth;

import java.util.Map;

public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

	private final Map<String, Object> attributes;

	public KakaoOAuth2UserInfo(Map<String, Object> attributes) {

		this.attributes = attributes;
	}

	@Override
	public String getProviderId() {

		return attributes.get("id").toString();
	}

	@Override
	public String getEmail() {

		Map<String, Object> account =
			(Map<String, Object>) attributes.get("kakao_account");
		return (String) account.get("email");
	}

	@Override
	public String getNickname() {

		Map<String, Object> profile =
			(Map<String, Object>) ((Map<String, Object>) attributes.get("kakao_account")).get("profile");
		return (String) profile.get("nickname");
	}
}
