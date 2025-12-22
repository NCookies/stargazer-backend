package xyz.ncookie.stargazer.domain.stargazing.service;

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import xyz.ncookie.stargazer.domain.stargazing.dto.GeminiRequest;
import xyz.ncookie.stargazer.domain.stargazing.dto.GeminiResponse;
import xyz.ncookie.stargazer.domain.stargazing.dto.OpenWeatherForecastResponse;
import xyz.ncookie.stargazer.domain.stargazing.dto.OpenWeatherResponse;
import xyz.ncookie.stargazer.domain.stargazing.dto.StargazingForecastResponse;
import xyz.ncookie.stargazer.domain.stargazing.dto.StargazingRequest;
import xyz.ncookie.stargazer.domain.stargazing.dto.StargazingResponse;

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

	private final LightPollutionService lightPollutionService;

	/**
	 * 특정 시점(현재 또는 미래)의 관측 적합도 상세 분석
	 */
	public StargazingResponse getAnalyze(StargazingRequest request) {
		log.info("Analyzing stargazing request {}", request);

		// 1. 파싱
		ZonedDateTime targetDateTime = ZonedDateTime.of(
			LocalDate.parse(request.date()),
			LocalTime.parse(request.time()),
			ZoneId.of("Asia/Seoul")
		);
		// ZonedDateTime targetDateTime = ZonedDateTime.of(
		// 	LocalDate.parse("2025-12-20"),
		// 	LocalTime.parse("21:00"),
		// 	ZoneId.of("Asia/Seoul")
		// );

		// 2. 외부 데이터 수집
		OpenWeatherResponse weatherData = fetchWeatherData(request.lat(), request.lon(), targetDateTime);

		StarAnalysisResult result = analyzeStargazingConditions(request.lat(), request.lon(), targetDateTime, weatherData);

		GeminiAnalysisResult aiResult = getGeminiAnalysis(
			result.finalScore(),
			request.lat(),
			request.lon(),
			weatherData,
			result.astro(),
			result.bortleClass()
		);

		return new StargazingResponse(
			targetDateTime.toLocalDate().toString(),
			targetDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
			aiResult.finalScore(),
			aiResult.comment(),
			new StargazingResponse.WeatherInfo(
				weatherData.clouds().all(),
				(int) weatherData.main().humidity(),
				getVisibilityText(weatherData.visibility())
			),
			new StargazingResponse.AstronomyInfo(
				getMoonPhaseName(result.astro().moonPhaseDegree()),
				result.astro().moonRiseTime(),
				result.astro().sunsetTime()
			),
			new StargazingResponse.LightPollutionInfo(
				"Class " + result.bortleClass(),
				getBrightnessText(result.bortleClass()),
				getLimitingMagText(result.bortleClass())
			)
		);
	}

	/**
	 * 주간 예보 조회 (5일 / 3시간 간격)
	 */
	public StargazingForecastResponse getForecast(double lat, double lon) {

		OpenWeatherForecastResponse rawData = fetchRawForecast(lat, lon);

		if (rawData == null || rawData.list() == null) {
			return new StargazingForecastResponse(List.of());
		}

		// 데이터 가공 (밤 시간대 필터링 및 그룹화)
		Map<String, List<StargazingForecastResponse.HourlyForecast>> groupedData = new LinkedHashMap<>();

		for (OpenWeatherForecastResponse.Item item : rawData.list()) {
			ZonedDateTime itemTime = ZonedDateTime.ofInstant(
				java.time.Instant.ofEpochSecond(item.dt()), ZoneId.of("Asia/Seoul")
			);

			// 태양 고도 체크 (이건 반복문 최적화를 위해 여기서 먼저 체크)
			SunPosition sunPos = SunPosition.compute().at(lat, lon).on(itemTime).execute();
			if (sunPos.getAltitude() > -6.0) continue; // 낮이면 스킵

			// 예보 데이터를 공통 포맷(OpenWeatherResponse)으로 변환
			OpenWeatherResponse tempWeather = convertForecastItemToWeather(item);

			// 공통 분석 메서드 호출! (getAnalyze와 똑같은 로직 적용됨)
			StarAnalysisResult result = analyzeStargazingConditions(lat, lon, itemTime, tempWeather);

			// DTO 생성
			StargazingForecastResponse.HourlyForecast hourlyDto = new StargazingForecastResponse.HourlyForecast(
				itemTime.format(DateTimeFormatter.ofPattern("HH:mm")),
				result.finalScore(), // 일관성 있는 점수!
				String.format("%.1f등급", 6.0 - (item.clouds().all() / 20.0)),
				item.clouds().all(),
				getMoonPhaseName(result.astro().moonPhaseDegree())
			);

			// 날짜별 그룹화
			String dateKey = itemTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			groupedData.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(hourlyDto);
		}

		// 최종 응답 변환 (Map -> List)
		List<StargazingForecastResponse.DailyForecast> dailyList = groupedData.entrySet().stream()
			.map(entry -> new StargazingForecastResponse.DailyForecast(entry.getKey(), entry.getValue()))
			.toList();

		return new StargazingForecastResponse(dailyList);
	}

	// =========================================================================
	//  Private Helper Methods
	// =========================================================================

	private record StarAnalysisResult(
		int finalScore,          // 최종 점수 (광해 반영됨)
		int weatherScore,        // 순수 기상 점수
		int bortleClass,         // 광해 등급
		RawAstronomyData astro   // 천문 데이터 (달, 일몰 등)
	) {}

	/*
	 * 핵심 공통 메서드
	 */
	private StarAnalysisResult analyzeStargazingConditions(double lat, double lon, ZonedDateTime dateTime, OpenWeatherResponse weather) {

		// [안전장치] 낮인지 밤인지 체크 (태양 고도)
		SunPosition sunPos = SunPosition.compute().at(lat, lon).on(dateTime).execute();
		boolean isDaytime = sunPos.getAltitude() > -6.0; // 시민박명(-6도) 이상이면 '낮'으로 간주

		// 천문 데이터 계산 (SunCalc)
		RawAstronomyData astro = calculateRawAstronomy(lat, lon, dateTime);

		// 광해 등급 조회 (CSV 데이터)
		int realBortle = lightPollutionService.getBortleClass(lat, lon);

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


	/**
	 * ✅ [공통 추출] OpenWeatherMap Forecast API 원본 데이터 호출
	 * - getForecast와 getAnalyze(미래 조회 시)에서 공통으로 사용
	 */
	private OpenWeatherForecastResponse fetchRawForecast(double lat, double lon) {
		String url = String.format(
			"https://api.openweathermap.org/data/2.5/forecast?lat=%f&lon=%f&appid=%s&units=metric",
			lat, lon, weatherApiKey
		);
		try {
			return restTemplate.getForObject(url, OpenWeatherForecastResponse.class);
		} catch (Exception e) {
			log.error("Forecast API 호출 실패", e);
			return null;
		}
	}

	/**
	 * 날씨 데이터 확보 전략
	 * - TargetTime이 현재와 가까우면: Current Weather API (/weather)
	 * - TargetTime이 미래면: Forecast API (/forecast) 호출 후 가장 가까운 시간대 추출
	 */
	private OpenWeatherResponse fetchWeatherData(double lat, double lon, ZonedDateTime targetDateTime) {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));

		// "지금"과 차이가 1시간 이내라면 -> 실시간 날씨 (/weather) 사용
		if (Math.abs(java.time.Duration.between(now, targetDateTime).toMinutes()) < 60) {
			return fetchCurrentWeather(lat, lon);
		}

		// 미래의 특정 시간이라면 -> 예보 데이터 (/forecast) 사용
		return fetchFutureWeather(lat, lon, targetDateTime);
	}

	private OpenWeatherResponse fetchCurrentWeather(double lat, double lon) {
		String url = String.format(
			"https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric",
			lat, lon, weatherApiKey
		);
		try {
			return restTemplate.getForObject(url, OpenWeatherResponse.class);
		} catch (Exception e) {
			log.error("실시간 날씨 API 실패", e);
			return createDummyWeather();
		}
	}

	private OpenWeatherResponse fetchFutureWeather(double lat, double lon, ZonedDateTime targetTime) {
		// 1. [공통 메서드 호출] Forecast API 데이터 가져오기
		OpenWeatherForecastResponse forecast = fetchRawForecast(lat, lon);

		if (forecast == null || forecast.list() == null) {
			return createDummyWeather();
		}

		// 2. 가장 가까운 시간대의 데이터 찾기 (Nearest Neighbor Search)
		OpenWeatherForecastResponse.Item bestMatch = null;
		long minDiff = Long.MAX_VALUE;

		for (OpenWeatherForecastResponse.Item item : forecast.list()) {
			ZonedDateTime itemTime = ZonedDateTime.ofInstant(
				java.time.Instant.ofEpochSecond(item.dt()), ZoneId.of("Asia/Seoul")
			);

			long diff = Math.abs(java.time.Duration.between(itemTime, targetTime).toMinutes());
			if (diff < minDiff) {
				minDiff = diff;
				bestMatch = item;
			}
		}

		if (bestMatch != null) {
			return convertForecastItemToWeather(bestMatch);
		}

		return createDummyWeather();
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

	private GeminiAnalysisResult getGeminiAnalysis(
		int finalScore,
		double lat,
		double lon,
		OpenWeatherResponse w,
		RawAstronomyData a,
		int bortleClass
	) {

		String addressName = getAddressName(lat, lon);

		String prompt = String.format("""
			너는 천체 관측 예보 전문가야. 아래 제공된 **확정 데이터(Fact)**를 바탕으로 사용자에게 관측 조언을 해줘.
		
			[관측지 정보]
			- 주소: %s
			- **광해 등급: Bortle Class %d** (정밀 지도 데이터 기반, 1~9등급)
			- 특징:
			  * Class 1~4: 별이 쏟아지는 시골/산간 지역 (관측 최적)
			  * Class 5~6: 교외 지역, 밝은 별 위주 관측 가능
			  * Class 7~9: 도심지, 행성/달 위주 관측 가능 (광해 심함)
		
			[기상 및 천문 데이터]
			- **최종 관측 점수: %d점** (기상과 광해 페널티가 이미 반영된 최종값)
			- 하늘 상태: 구름 %d%%, 시정 %s
			- 달 상태: %s (밝기 %.2f)
		
			[지시사항]
			1. **점수 계산 금지**: 입력된 '최종 관측 점수'를 그대로 사용해. 절대 네가 다시 계산하지 마.
			2. **코멘트 작성**:
			   - 점수가 높으면(70점 이상): "별이 아주 잘 보입니다", "은하수 관측 도전!" 등의 긍정적 멘트.
			   - 점수가 낮으면(40점 미만): 원인을 콕 집어 말해줘. (예: "서울 도심이라 너무 밝네요", "구름이 많아서 아쉽네요")
			   - 광해 등급(Bortle)에 맞춰 현실적인 조언을 해줘. (예: Class 8이면 "별보다는 달이나 목성을 보세요"라고 추천)
			3. 아래 JSON 포맷으로 응답해.
		
			{
			  "final_score": %d,
			  "comment": "한 줄 평 (자연스럽고 친절하게)",
			  "bortle": "Class %d",
			  "brightness": "광해 등급에 따른 밝기 멘트 (예: 매우 어두움/보통/매우 밝음)",
			  "limiting_mag": "광해 등급에 따른 한계등급 추정치 (예: 6.0등급 / 4.5등급 / 3.0등급)"
			}
			""",
			addressName,            // 주소
			bortleClass,             // 광해 등급 (CSV 값)
			finalScore,
			w.clouds().all(),       // 구름
			getVisibilityText(w.visibility()), // 시정 텍스트
			getMoonPhaseName(a.moonPhaseDegree()), // 달 이름
			a.moonFraction(),       // 달 밝기
			finalScore,   // JSON에 넣을 점수 (위와 동일)
			bortleClass              // JSON에 넣을 Bortle (위와 동일)
		);

		String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiKey;

		try {
			GeminiResponse response = restTemplate.postForObject(url, GeminiRequest.of(prompt), GeminiResponse.class);
			if (response == null) return new GeminiAnalysisResult(finalScore, "분석 불가", "-", "-", "-");

			String jsonText = response.getText().replace("```json", "").replace("```", "").trim();
			JsonNode root = objectMapper.readTree(jsonText);

			return new GeminiAnalysisResult(
				root.path("final_score").asInt(finalScore), // AI가 계산한 최종 점수 사용
				root.path("comment").asString("밤하늘을 올려다보세요."),
				root.path("bortle").asString("알 수 없음"),
				root.path("brightness").asString("보통"),
				root.path("limiting_mag").asString("4.0등급")
			);

		} catch (Exception e) {
			log.error("Gemini Error", e);
			// 에러 시 기상 점수 그대로 반환
			return new GeminiAnalysisResult(finalScore, "AI 연결 지연", "Class ?", "알 수 없음", "?등급");
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

	private String getAddressName(double lat, double lon) {
		// OpenWeatherMap Reverse Geocoding API (무료)
		String url = String.format(
			"http://api.openweathermap.org/geo/1.0/reverse?lat=%f&lon=%f&limit=1&appid=%s",
			lat, lon, weatherApiKey
		);

		try {
			// 응답용 임시 Record (내부 클래스로 정의)
			@JsonIgnoreProperties(ignoreUnknown = true)
			record GeoResult(String name, String country, String state) {} // state가 'Gyeonggi-do' 같은 정보

			GeoResult[] results = restTemplate.getForObject(url, GeoResult[].class);
			if (results != null && results.length > 0) {
				GeoResult r = results[0];
				// 예: "Yangpyeong-gun, KR" 형태로 반환
				return (r.name() != null ? r.name() : "") +
					(r.state() != null ? ", " + r.state() : "") +
					", " + r.country();
			}
		} catch (Exception e) {
			log.error("주소 변환 실패", e);
		}
		return "Unknown Location";
	}

	// 광해 페널티 계산 공통 로직
	private int calculateLightPollutionPenalty(int bortleClass) {
		if (bortleClass >= 8) return 50;      // 서울 도심 (최악)
		if (bortleClass >= 7) return 40;
		if (bortleClass >= 6) return 25;      // 수도권/신도시
		if (bortleClass == 5) return 10;      // 교외
		return 0;                             // 시골 (감점 없음)
	}

	// Bortle 등급에 따른 텍스트 헬퍼 (AI 의존도 낮추기 위해)
	private String getBrightnessText(int bortle) {
		if (bortle <= 2) return "매우 어두움";
		if (bortle <= 4) return "어두움";
		if (bortle <= 6) return "보통";
		return "매우 밝음";
	}

	private String getLimitingMagText(int bortle) {
		// 대략적인 한계등급 추정
		if (bortle <= 2) return "6.5등급";
		if (bortle <= 4) return "6.0등급";
		if (bortle <= 5) return "5.5등급";
		if (bortle <= 7) return "4.5등급";
		return "3.0등급";
	}

	private OpenWeatherResponse convertForecastItemToWeather(OpenWeatherForecastResponse.Item item) {
		return new OpenWeatherResponse(
			new OpenWeatherResponse.Main(item.main().temp(), item.main().humidity()),
			new OpenWeatherResponse.Clouds(item.clouds().all()),
			(item.visibility() != null) ? item.visibility() : 10000,
			null
		);
	}

	// 더미 데이터 생성 (에러 시 Fallback)
	private OpenWeatherResponse createDummyWeather() {
		return new OpenWeatherResponse(
			new OpenWeatherResponse.Main(0.0, 50.0),
			new OpenWeatherResponse.Clouds(100), // 구름 100% (관측 불가 처리)
			10000, null
		);
	}
}
