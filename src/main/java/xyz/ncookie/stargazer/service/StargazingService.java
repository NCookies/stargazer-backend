package xyz.ncookie.stargazer.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.shredzone.commons.suncalc.MoonIllumination;
import org.shredzone.commons.suncalc.MoonPosition;
import org.shredzone.commons.suncalc.MoonTimes;
import org.shredzone.commons.suncalc.SunTimes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import xyz.ncookie.stargazer.dto.GeminiRequest;
import xyz.ncookie.stargazer.dto.GeminiResponse;
import xyz.ncookie.stargazer.dto.OpenWeatherResponse;
import xyz.ncookie.stargazer.dto.StargazingRequest;
import xyz.ncookie.stargazer.dto.StargazingResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class StargazingService {

	@Value("${weather.api.key}")
	private String weatherApiKey;

	@Value("${gemini.api.key}")
	private String geminiKey;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	// 점수 계산용 상수
	private static final int SCORE_MAX = 100;
	private static final int VISIBILITY_GOOD = 10000;
	private static final int VISIBILITY_BAD = 5000;
	private static final double MOON_FRACTION_THRESHOLD = 0.3;

	/**
	 * 메인 분석 로직
	 */
	public StargazingResponse analyze(StargazingRequest request) {
		log.info("Analyzing stargazing request {}", request);

		// 1. 파싱
		ZonedDateTime targetDateTime = ZonedDateTime.of(
			LocalDate.parse(request.date()),
			LocalTime.parse(request.time()),
			ZoneId.of("Asia/Seoul")
		);

		// 2. 외부 데이터 수집
		OpenWeatherResponse weatherData = fetchWeatherData(request.lat(), request.lon());
		RawAstronomyData rawAstro = calculateRawAstronomy(request.lat(), request.lon(), targetDateTime);

		// 3. [1차 점수] 기상 및 천문 조건만 고려한 점수 (광해 미반영 상태)
		int weatherScore = calculateWeatherScore(weatherData, rawAstro);

		// 4. [2차 보정] AI에게 위치 기반 광해 페널티 적용 요청 (최종 점수 도출)
		GeminiAnalysisResult aiResult = getGeminiAnalysis(weatherScore, request.lat(), request.lon(), weatherData, rawAstro);

		// 5. 응답 생성 (점수는 AI가 보정한 finalScore 사용)
		return new StargazingResponse(
			aiResult.finalScore(), // 👈 AI가 수정한 점수 반영!
			aiResult.comment(),
			new StargazingResponse.WeatherInfo(
				weatherData.clouds().all(),
				(int) weatherData.main().humidity(),
				getVisibilityText(weatherData.visibility())
			),
			new StargazingResponse.AstronomyInfo(
				getMoonPhaseName(rawAstro.moonPhaseDegree()),
				rawAstro.moonRiseTime(),
				rawAstro.sunsetTime()
			),
			new StargazingResponse.LightPollutionInfo(
				aiResult.bortleClass(),
				aiResult.brightness(),
				aiResult.limitingMag()
			)
		);
	}

	// =========================================================================
	//  Private Helper Methods
	// =========================================================================

	// 내부 계산용 데이터 묶음 (Record)
	private record RawAstronomyData(
		double moonFraction,    // 달 밝기 (0.0 ~ 1.0)
		double moonPhaseDegree, // 달 위상 각도
		double moonAltitude,    // 달 고도
		String moonRiseTime,    // 월출 시간 (HH:mm)
		String sunsetTime       // 일몰 시간 (HH:mm)
	) {}

	// 1. 날씨 데이터 가져오기
	private OpenWeatherResponse fetchWeatherData(double lat, double lon) {
		String url = String.format(
			"https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric",
			lat, lon, weatherApiKey
		);

		try {
			return restTemplate.getForObject(url, OpenWeatherResponse.class);
		} catch (Exception e) {
			log.error("날씨 API 호출 실패: {}", e.getMessage());
			// Fail-safe: 더미 데이터 반환
			return new OpenWeatherResponse(
				new OpenWeatherResponse.Main(0.0, 50.0),
				new OpenWeatherResponse.Clouds(100),
				3000,
				null
			);
		}
	}

	// 2. 천문 데이터 정밀 계산 (SunCalc)
	private RawAstronomyData calculateRawAstronomy(double lat, double lon, ZonedDateTime dateTime) {
		// 월령 및 위상
		MoonIllumination moonIllum = MoonIllumination.compute().on(dateTime).execute();

		// 달 위치 (고도)
		MoonPosition moonPos = MoonPosition.compute().at(lat, lon).on(dateTime).execute();

		// 일몰 시간
		SunTimes sunTimes = SunTimes.compute().on(dateTime).at(lat, lon).execute();
		String sunset = (sunTimes.getSet() != null)
			? sunTimes.getSet().withZoneSameInstant(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("HH:mm"))
			: "--:--";

		// 월출 시간
		MoonTimes moonTimes = MoonTimes.compute().on(dateTime).at(lat, lon).execute();
		String moonrise = (moonTimes.getRise() != null)
			? moonTimes.getRise().withZoneSameInstant(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("HH:mm"))
			: "뜨지 않음";

		return new RawAstronomyData(
			moonIllum.getFraction(),
			moonIllum.getPhase(),
			moonPos.getAltitude(),
			moonrise,
			sunset
		);
	}

	// 3. 점수 계산 로직 (복구됨)
	private int calculateWeatherScore(OpenWeatherResponse weather, RawAstronomyData astro) {
		int score = SCORE_MAX;

		double cloudCover = weather.clouds().all();
		double visibility = weather.visibility();

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
		if (astro.moonAltitude > 0 && astro.moonFraction > MOON_FRACTION_THRESHOLD) {
			score -= (int) (astro.moonFraction * 40);
		}

		return Math.max(0, score); // 0점 미만 방지
	}

	// 4. Gemini AI 분석 요청 (JSON 파싱 포함)
	private record GeminiAnalysisResult(int finalScore, String comment, String bortleClass, String brightness, String limitingMag) {}

	private GeminiAnalysisResult getGeminiAnalysis(int weatherScore, double lat, double lon, OpenWeatherResponse w, RawAstronomyData a) {
		// 프롬프트 강화: 광해 페널티 로직 명시
		String prompt = String.format("""
            너는 천체 관측 전문가야. 다음 위치의 관측 조건을 분석해줘.
            
            [입력 데이터]
            - 위치: 위도 %.4f, 경도 %.4f
            - 기상/천문 기반 잠정 점수: %d점 (100점 만점)
            - 날씨: 구름 %d%%, 시정 %s
            - 달: %s (위상 %.2f)
            
            [지시사항]
            1. 위도/경도를 보고 해당 지역의 광해(Light Pollution) 수준(Bortle Scale)을 추정해.
            2. 도심지이거나 광해가 심하다면, 입력된 '잠정 점수'에서 과감하게 점수를 깎아 '최종 점수(final_score)'를 계산해. (서울 도심이면 30~50점 이상 감점 가능)
            3. 결과를 아래 JSON 포맷으로만 응답해.
            
            {
              "final_score": 45,
              "comment": "서울 도심이라 밝아서 별이 잘 안 보여요. 하지만 달은 선명하네요.",
              "bortle": "Class 8 (도심)",
              "brightness": "매우 밝음",
              "limiting_mag": "3.0등급"
            }
            """, lat, lon, weatherScore, w.clouds().all(), getVisibilityText(w.visibility()), getMoonPhaseName(a.moonPhaseDegree()), a.moonFraction);

		String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiKey;

		try {
			GeminiResponse response = restTemplate.postForObject(url, GeminiRequest.of(prompt), GeminiResponse.class);
			if (response == null) return new GeminiAnalysisResult(weatherScore, "분석 불가", "-", "-", "-");

			String jsonText = response.getText().replace("```json", "").replace("```", "").trim();
			JsonNode root = objectMapper.readTree(jsonText);

			return new GeminiAnalysisResult(
				root.path("final_score").asInt(weatherScore), // AI가 계산한 최종 점수 사용
				root.path("comment").asText("밤하늘을 올려다보세요."),
				root.path("bortle").asText("알 수 없음"),
				root.path("brightness").asText("보통"),
				root.path("limiting_mag").asText("4.0등급")
			);

		} catch (Exception e) {
			log.error("Gemini Error", e);
			// 에러 시 기상 점수 그대로 반환
			return new GeminiAnalysisResult(weatherScore, "AI 연결 지연", "Class ?", "알 수 없음", "?등급");
		}
	}

	// 5. 시정 거리 텍스트 변환 (복구됨)
	private String getVisibilityText(int visibilityMeters) {
		if (visibilityMeters >= 20000) return "최상";
		if (visibilityMeters >= 10000) return "매우 좋음";
		if (visibilityMeters >= 5000) return "좋음";
		if (visibilityMeters >= 2000) return "보통";
		return "나쁨";
	}

	// 6. 달 이름 변환 로직 (복구됨)
	private String getMoonPhaseName(double phase) {
		// SunCalc phase: -180 ~ 180 (0: New Moon, 180: Full Moon)
		double p = Math.abs(phase);

		if (p < 10) return "삭 (New Moon)";
		if (p < 80) return "초승달 (Waxing Crescent)";
		if (p < 100) return "상현달 (First Quarter)"; // 90도 근처
		if (p < 170) return "채워가는 달 (Waxing Gibbous)";
		return "보름달 (Full Moon)";
	}
}
