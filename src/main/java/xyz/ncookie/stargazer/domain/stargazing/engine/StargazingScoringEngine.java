package xyz.ncookie.stargazer.domain.stargazing.engine;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.shredzone.commons.suncalc.SunPosition;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.stargazing.component.AstronomyCalculator;
import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherResponse;
import xyz.ncookie.stargazer.domain.stargazing.provider.LightPollutionDataProvider;
import xyz.ncookie.stargazer.domain.stargazing.model.RawAstronomyData;
import xyz.ncookie.stargazer.domain.stargazing.model.StarAnalysisResult;

@Component
@RequiredArgsConstructor
public class StargazingScoringEngine {

	private final LightPollutionDataProvider lightPollutionDataProvider;
	private final AstronomyCalculator astronomyCalculator;

	// 점수 기준 상수
	private static final int PERFECT_SCORE = 100;
	private static final double VISIBILITY_THRESHOLD_GOOD = 10000.0; // 10km
	private static final double VISIBILITY_THRESHOLD_BAD = 5000.0;   // 5km

	/**
	 * 별 관측 점수 계산 (v2.1 정밀 로직 적용)
	 */
	public StarAnalysisResult calculateScore(double lat, double lon, ZonedDateTime dateTime, OpenWeatherResponse weather) {

		// 데이터 준비
		RawAstronomyData astro = astronomyCalculator.calculate(lat, lon, dateTime);
		int realBortle = lightPollutionDataProvider.getBortleClass(lat, lon);
		double humidity = weather.main().humidity();
		double visibility = (weather.visibility() != null) ? weather.visibility() : 10000;

		// [안전장치] 낮인지 밤인지 체크 (태양 고도)
		SunPosition sunPos = SunPosition.compute().at(lat, lon).on(dateTime).execute();
		if (sunPos.getAltitude() > -6.0) { // 시민박명(-6도) 이상이면 '낮'
			return new StarAnalysisResult(0, List.of("해가 떠 있어 별이 보이지 않습니다."), realBortle, astro);
		}

		// 구름 체크 (Fast-Fail: 70% 이상이면 즉시 0점)
		double cloudCover = weather.clouds().all();
		if (cloudCover >= 70) {
			return new StarAnalysisResult(0, List.of(String.format("구름이 하늘을 덮었습니다 (%.0f%%)", cloudCover)), realBortle, astro);
		}

		// 점수 계산 시작 (100점에서 차감)
		double currentScore = PERFECT_SCORE;
		List<String> deductionReasons = new ArrayList<>();

		// --------------------------------------------------------
		// A. 구름 정밀 감점 (10% 이상부터 1%당 1점)
		// --------------------------------------------------------
		if (cloudCover > 10) {
			double cloudPenalty = cloudCover - 10;
			currentScore -= cloudPenalty;
			deductionReasons.add(String.format("구름 %.0f%% (-%d점)", cloudCover, (int) cloudPenalty));
		}

		// --------------------------------------------------------
		// B. 습도 정밀 감점 (60% 이상부터 2%당 1점)
		// --------------------------------------------------------
		if (humidity > 60) {
			double humidityPenalty = (humidity - 60) / 2.0;
			currentScore -= humidityPenalty;
			deductionReasons.add(String.format("습도 %.0f%% (-%d점)", humidity, (int) Math.round(humidityPenalty)));
		}

		// --------------------------------------------------------
		// C. 시정(Visibility) 감점 (단계별 감점)
		// --------------------------------------------------------
		if (visibility < VISIBILITY_THRESHOLD_BAD) { // 5km 미만 (안개/미세먼지 매우 나쁨)
			currentScore -= 20;
			deductionReasons.add("시야가 매우 흐림 (5km 미만, -20점)");
		} else if (visibility < VISIBILITY_THRESHOLD_GOOD) { // 10km 미만 (약간 뿌염)
			currentScore -= 10;
			deductionReasons.add("시야가 다소 흐림 (10km 미만, -10점)");
		}

		// --------------------------------------------------------
		// D. 달 정밀 감점 (떠있을 때만, 밝기 비례 최대 -30점)
		// --------------------------------------------------------
		if (astro.moonAltitude() > 0) { // 달이 지평선 위에 있음
			double moonPenalty = astro.moonFraction() * 30; // 0.0 ~ 1.0 * 30
			if (moonPenalty >= 1) {
				currentScore -= moonPenalty;
				deductionReasons.add(String.format("달 밝기 %.0f%% (-%d점)", astro.moonFraction() * 100, (int) Math.round(moonPenalty)));
			}
		}

		// --------------------------------------------------------
		// E. 광해 정밀 감점 ((등급-1) * 6.25)
		// --------------------------------------------------------
		if (realBortle > 1) {
			double lpPenalty = (realBortle - 1) * 6.25;
			currentScore -= lpPenalty;
			deductionReasons.add(String.format("광해 등급 Class %d (-%d점)", realBortle, (int) Math.round(lpPenalty)));
		}

		// 최종 점수 보정 (0점 미만 방지)
		int finalScore = (int) Math.round(Math.max(0, currentScore));

		// 만약 100점이라면 메시지 추가
		if (finalScore == 100 && deductionReasons.isEmpty()) {
			deductionReasons.add("완벽한 관측 조건입니다!");
		}

		return new StarAnalysisResult(finalScore, deductionReasons, realBortle, astro);
	}
}
