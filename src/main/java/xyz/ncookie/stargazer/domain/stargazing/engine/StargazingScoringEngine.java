package xyz.ncookie.stargazer.domain.stargazing.engine;

import java.time.ZonedDateTime;

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

	// 점수 계산용 상수
	private static final int SCORE_MAX = 100;
	private static final int VISIBILITY_GOOD = 10000;
	private static final int VISIBILITY_BAD = 5000;
	private static final double MOON_FRACTION_THRESHOLD = 0.3;

	public StarAnalysisResult calculateScore(double lat, double lon, ZonedDateTime dateTime, OpenWeatherResponse weather) {

		// [안전장치] 낮인지 밤인지 체크 (태양 고도)
		SunPosition sunPos = SunPosition.compute().at(lat, lon).on(dateTime).execute();
		boolean isDaytime = sunPos.getAltitude() > -6.0; // 시민박명(-6도) 이상이면 '낮'으로 간주

		// 천문 데이터 계산 (SunCalc)
		RawAstronomyData astro = astronomyCalculator.calculate(lat, lon, dateTime);

		// 광해 등급 조회 (CSV 데이터)
		int realBortle = lightPollutionDataProvider.getBortleClass(lat, lon);

		int finalScore;
		int weatherScore;

		if (isDaytime) {
			weatherScore = 0;
			finalScore = 0;
		} else {
			weatherScore = calculateWeatherScore(weather, astro);
			int penalty = calculateLightPollutionPenalty(realBortle);
			finalScore = Math.max(0, weatherScore - penalty);
		}

		return new StarAnalysisResult(finalScore, weatherScore, realBortle, astro);
	}


	private int calculateWeatherScore(OpenWeatherResponse weather, RawAstronomyData astro) {

		int score = SCORE_MAX;

		double cloudCover = weather.clouds().all();

		Integer rawVisibility = weather.visibility();
		double visibility = (rawVisibility != null) ? rawVisibility : 10000;

		// 1. 구름 감점 (가장 치명적)
		if (cloudCover > 10) {
			score -= (int) (cloudCover * 0.8);
		}

		// 2. 시정(Visibility) 감점
		if (visibility < VISIBILITY_BAD) { // 5km 미만
			score -= 30;
		} else if (visibility < VISIBILITY_GOOD) { // 10km 미만
			score -= 10;
		}

		// 3. 달 밝기 감점 (달이 지평선 위에 있고, 일정 밝기 이상일 때만)
		if (astro.moonAltitude() > 0 && astro.moonFraction() > MOON_FRACTION_THRESHOLD) {
			score -= (int) (astro.moonFraction() * 40);
		}

		return Math.max(0, score); // 0점 미만 방지
	}

	// 광해 페널티 계산 공통 로직
	private int calculateLightPollutionPenalty(int bortleClass) {

		if (bortleClass >= 8) return 50;      // 서울 도심 (최악)
		if (bortleClass >= 7) return 40;
		if (bortleClass >= 6) return 25;      // 수도권/신도시
		if (bortleClass == 5) return 10;      // 교외
		return 0;                             // 시골 (감점 없음)
	}

}
