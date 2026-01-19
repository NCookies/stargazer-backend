package xyz.ncookie.stargazer.global.security.oauth;

public interface OAuth2UserInfo {

	String getProviderId();
	String getEmail();
	String getNickname();
}
