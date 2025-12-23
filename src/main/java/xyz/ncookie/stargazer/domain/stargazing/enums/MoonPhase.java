package xyz.ncookie.stargazer.domain.stargazing.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MoonPhase {

	// 1. 주요 단계 (Main Phases)
	NEW_MOON("삭", "🌑", "달이 보이지 않음"),
	FULL_MOON("보름달", "🌕", "달이 가장 밝음"),

	// 2. 차오르는 달 (Waxing: New -> Full)
	WAXING_CRESCENT("초승달", "🌒", "오른쪽이 차오르는 눈썹달"),
	FIRST_QUARTER("상현달", "🌓", "오른쪽 반달"),
	WAXING_GIBBOUS("상현망간의 달", "🌔", "보름달로 가는 부푼 달"),

	// 3. 이지러지는 달 (Waning: Full -> New)
	WANING_GIBBOUS("하현망간의 달", "🌖", "왼쪽이 남은 부푼 달"),
	LAST_QUARTER("하현달", "🌗", "왼쪽 반달"),
	WANING_CRESCENT("그믐달", "🌘", "왼쪽만 남은 눈썹달");

	private final String title;       // 한글 명칭
	private final String icon;        // 아이콘 (이모지)
	private final String description; // 설명

	/**
	 * 각도(Phase Degree)를 기반으로 현재 위상을 반환하는 로직
	 * @param phase -180 ~ 180 (0: Full, ±180: New)
	 */
	public static MoonPhase calculate(double phase) {
		double absPhase = Math.abs(phase);

		// 1. 주요 단계 (Full & New) - 오차 범위 ±10도
		if (absPhase < 10) return FULL_MOON;
		if (absPhase > 170) return NEW_MOON;

		// 2. 양수 구간 (0 ~ 180): Full(0) -> New(180)로 가는 과정 (Waning, 이지러짐)
		if (phase > 0) {
			if (phase < 80) return WANING_GIBBOUS;  // 10 ~ 80
			if (phase < 100) return LAST_QUARTER;   // 80 ~ 100 (90 근처)
			return WANING_CRESCENT;                 // 100 ~ 170
		}

		// 3. 음수 구간 (-180 ~ 0): New(-180) -> Full(0)로 가는 과정 (Waxing, 차오름)
		// 주의: 음수이므로 0에 가까울수록(-10) 큽니다.
		else {
			if (phase > -80) return WAXING_GIBBOUS; // -80 ~ -10
			if (phase > -100) return FIRST_QUARTER; // -100 ~ -80 (-90 근처)
			return WAXING_CRESCENT;                 // -170 ~ -100
		}
	}
}
