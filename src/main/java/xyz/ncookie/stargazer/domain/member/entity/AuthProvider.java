package xyz.ncookie.stargazer.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthProvider {
	KAKAO("카카오"),
	NAVER("네이버"),
	GOOGLE("구글"),
	LOCAL("자체 회원")
	;

	private final String desc;
}
