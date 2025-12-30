package xyz.ncookie.stargazer.domain.stargazing.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum BortleGrade {

	// (Class, 밝기 설명, 한계등급 설명)
	CLASS_1(1, "매우 어두움", "6.5등급 이상"),
	CLASS_2(2, "매우 어두움", "6.5등급"),
	CLASS_3(3, "어두움",     "6.0등급"),
	CLASS_4(4, "어두움",     "6.0등급"),
	CLASS_5(5, "보통",       "5.5등급"),
	CLASS_6(6, "보통",       "4.5등급"), // 사용자 로직 반영 (Brightness: <=6 보통, Mag: <=7 4.5)
	CLASS_7(7, "매우 밝음",   "4.5등급"),
	CLASS_8(8, "매우 밝음",   "3.0등급"), // <=7 아님 -> 3.0
	CLASS_9(9, "매우 밝음",   "3.0등급");

	private final int score;
	private final String brightnessText;
	private final String limitingMagText;

	public static BortleGrade from(int score) {
		return Arrays.stream(values())
			.filter(b -> b.score == score)
			.findFirst()
			.orElse(CLASS_9); // 기본값: 도심지
	}
}
