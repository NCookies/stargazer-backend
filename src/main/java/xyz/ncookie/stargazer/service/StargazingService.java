package xyz.ncookie.stargazer.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.shredzone.commons.suncalc.MoonIllumination;
import org.shredzone.commons.suncalc.MoonPosition;
import org.shredzone.commons.suncalc.MoonTimes;
import org.shredzone.commons.suncalc.SunPosition;
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
import xyz.ncookie.stargazer.dto.OpenWeatherForecastResponse;
import xyz.ncookie.stargazer.dto.OpenWeatherResponse;
import xyz.ncookie.stargazer.dto.StargazingForecastResponse;
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
	public StargazingResponse getAnalyze(StargazingRequest request) {
		log.info("Analyzing stargazing request {}", request);

		// 1. 파싱
		ZonedDateTime targetDateTime = ZonedDateTime.of(
			LocalDate.parse(request.date()),
			// LocalDate.parse("2025-12-04"),
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

	public StargazingForecastResponse getForecast(double lat, double lon) {
		// 1. Forecast API 호출 (5일치 / 3시간 간격)
		String url = String.format(
			"https://api.openweathermap.org/data/2.5/forecast?lat=%f&lon=%f&appid=%s&units=metric",
			lat, lon, weatherApiKey
		);

		OpenWeatherForecastResponse rawData;
		try {
			rawData = restTemplate.getForObject(url, OpenWeatherForecastResponse.class);
		} catch (Exception e) {
			log.error("예보 API 호출 실패", e);
			return new StargazingForecastResponse(List.of()); // 빈 리스트 반환
		}

		if (rawData == null || rawData.list() == null) {
			return new StargazingForecastResponse(List.of());
		}

		// 2. 데이터 가공 (밤 시간대 필터링 및 그룹화)
		Map<String, List<StargazingForecastResponse.HourlyForecast>> groupedData = new LinkedHashMap<>();

		for (OpenWeatherForecastResponse.Item item : rawData.list()) {
			// 시간 파싱
			ZonedDateTime itemTime = ZonedDateTime.ofInstant(
				java.time.Instant.ofEpochSecond(item.dt()),
				ZoneId.of("Asia/Seoul")
			);

			// 천문 데이터 계산 (SunCalc)
			RawAstronomyData astro = calculateRawAstronomy(lat, lon, itemTime);

			// ☀️ 필터링: 해가 떠있으면(시민박명 포함) 관측 불가하므로 스킵
			// 천문박명(Astronimical Twilight) 기준: 태양 고도 -12도 미만이어야 별이 잘 보임
			// 하지만 조금 관대하게 -6도(시민박명 끝)부터 보여주기로 함 (야경 포함)
			SunPosition sunPos = SunPosition.compute().at(lat, lon).on(itemTime).execute();
			if (sunPos.getAltitude() > -6.0) {
				continue;
			}

			// 점수 계산 (기존 메서드 재활용!)
			// 단, Forecast API 구조에 맞춰 변환 필요
			OpenWeatherResponse.Main main = new OpenWeatherResponse.Main(item.main().temp(), item.main().humidity());
			OpenWeatherResponse.Clouds clouds = new OpenWeatherResponse.Clouds(item.clouds().all());
			// API마다 필드명이 달라서 임시 객체 생성 (점수 계산기 호환용)
			OpenWeatherResponse tempWeather = new OpenWeatherResponse(main, clouds, item.visibility(), null);

			int score = calculateWeatherScore(tempWeather, astro);

			// DTO 생성
			StargazingForecastResponse.HourlyForecast hourlyDto = new StargazingForecastResponse.HourlyForecast(
				itemTime.format(DateTimeFormatter.ofPattern("HH:mm")),
				score,
				String.format("%.1f등급", 6.0 - (item.clouds().all() / 20.0)), // 간단한 등급 추산 로직
				item.clouds().all(),
				getMoonPhaseName(astro.moonPhaseDegree())
			);

			// 날짜별 그룹화
			String dateKey = itemTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			groupedData.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(hourlyDto);
		}

		// 3. 최종 응답 변환 (Map -> List)
		List<StargazingForecastResponse.DailyForecast> dailyList = groupedData.entrySet().stream()
			.map(entry -> new StargazingForecastResponse.DailyForecast(entry.getKey(), entry.getValue()))
			.toList();

		return new StargazingForecastResponse(dailyList);
	}

	// =========================================================================
	//  Private Helper Methods
	// =========================================================================

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

	// 내부 계산용 데이터 묶음 (Record)
	private record RawAstronomyData(
		double moonFraction,    // 달 밝기 (0.0 ~ 1.0)
		double moonPhaseDegree, // 달 위상 각도
		double moonAltitude,    // 달 고도
		String moonRiseTime,    // 월출 시간 (HH:mm)
		String sunsetTime       // 일몰 시간 (HH:mm)
	) {}

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

	// 3. 점수 계산 로직
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

	// 6. 달 이름 변환 로직
	private String getMoonPhaseName(double phase) {
		// SunCalc phase: -180 ~ 180
		// 0: Full Moon (보름달) ⚠️ 실제 라이브러리 정의
		// ±90: Quarter Moons (상현/하현)
		// ±180: New Moon (삭) ⚠️ 실제 라이브러리 정의

		log.info("달 위상 각도 계산값: {}", phase);

		// 1. 주요 단계 (범위를 좁게 잡음: ±10도)
		if (Math.abs(phase) < 10) return "FULL_MOON";             // 보름달 (0도 근처)
		if (Math.abs(phase) > 170) return "NEW_MOON";             // 삭 (±180도 근처)

		// 2. 차오르는 달 (Waxing, 양수 구간 0 ~ 180)
		// 0도(보름)에서 시작해서 180도(삭)로 가는 과정
		if (phase > 0) {
			if (phase < 80) return "WANING_GIBBOUS";              // 하현망간의 달 (보름→하현)
			if (phase < 100) return "LAST_QUARTER";               // 하현달 (90도 근처)
			return "WANING_CRESCENT";                             // 그믐달 (하현→삭)
		}

		// 3. 이지러지는 달 (Waning, 음수 구간 -180 ~ 0)
		// -180도(삭)에서 시작해서 0도(보름)로 가는 과정
		if (phase > -80) return "WAXING_GIBBOUS";                 // 상현망간의 달 (상현→보름)
		if (phase > -100) return "FIRST_QUARTER";                 // 상현달 (-90도 근처)
		return "WAXING_CRESCENT";                                 // 초승달 (삭→상현)
	}
}
