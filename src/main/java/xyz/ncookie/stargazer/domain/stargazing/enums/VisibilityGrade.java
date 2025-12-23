package xyz.ncookie.stargazer.domain.stargazing.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VisibilityGrade {

	BEST("최상", 20000),
	VERY_GOOD("매우 좋음", 10000),
	GOOD("좋음", 5000),
	NORMAL("보통", 2000),
	BAD("나쁨", 0);

	private final String label;
	private final int minMeters;

	public static VisibilityGrade from(int visibilityMeters) {
		// 내림차순으로 검사 (20000 -> 10000 -> ...)
		if (visibilityMeters >= BEST.minMeters) return BEST;
		if (visibilityMeters >= VERY_GOOD.minMeters) return VERY_GOOD;
		if (visibilityMeters >= GOOD.minMeters) return GOOD;
		if (visibilityMeters >= NORMAL.minMeters) return NORMAL;
		return BAD;
	}
}
