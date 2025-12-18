package xyz.ncookie.stargazer.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.shredzone.commons.suncalc.MoonIllumination;
import org.shredzone.commons.suncalc.MoonPosition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.dto.GeminiRequest;
import xyz.ncookie.stargazer.dto.GeminiResponse;
import xyz.ncookie.stargazer.dto.OpenWeatherResponse;
import xyz.ncookie.stargazer.dto.StargazingRequest;
import xyz.ncookie.stargazer.dto.StargazingResponse;

@Service
@Slf4j
public class StargazingService {

	@Value("${weather.api.key}")
	private String weatherApiKey;

	@Value("${gemini.api.key}")
	private String geminiKey;

	private final RestTemplate restTemplate = new RestTemplate();

	public StargazingResponse analyze(StargazingRequest request) {

		LocalDate date = LocalDate.parse(request.date()); // "2024-05-20"
		LocalTime time = LocalTime.parse(request.time()); // "22:00"
		ZonedDateTime targetDateTime = ZonedDateTime.of(date, time, ZoneId.of("Asia/Seoul"));

		String url = String.format(
			"https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric",
			request.lat(), request.lon(), weatherApiKey
		);

		OpenWeatherResponse weatherData;
		try {
			weatherData = restTemplate.getForObject(url, OpenWeatherResponse.class);
		} catch (Exception e) {
			// 에러 나면 기본값 (혹은 에러 처리)
			log.error("날씨 API 호출 실패: " + e.getMessage());
			// 실패 시 더미 데이터라도 넣어서 서비스 중단 방지
			weatherData = new OpenWeatherResponse(
				new OpenWeatherResponse.Main(20.0, 50.0),
				new OpenWeatherResponse.Clouds(100), // 실패하면 구름 많음으로 처리
				5000,
				null
			);
		}

		double cloudCover = weatherData.clouds().all(); // 0~100%
		double humidity = weatherData.main().humidity();
		double visibility = weatherData.visibility(); // 미터 단위

		// 월령 계산 (0.0 = 삭, 1.0 = 보름달)
		MoonIllumination moonIllum = MoonIllumination.compute()
			.on(targetDateTime)
			.execute();
		double moonFraction = moonIllum.getFraction(); // 0.0 ~ 1.0
		double moonPhaseDegree = moonIllum.getPhase(); // 달의 위상 각도

		// 달의 고도 계산 (지평선 아래에 있는지 확인용)
		MoonPosition moonPos = MoonPosition.compute()
			.at(request.lat(), request.lon())
			.on(targetDateTime)
			.execute();
		double moonAltitude = moonPos.getAltitude(); // 양수면 떠있음, 음수면 짐

		// 달의 상태 텍스트로 변환
		String moonPhaseName = getMoonPhaseName(moonPhaseDegree);

		// 점수 계산 로직 (간단 버전)
		int score = 100;

		// 구름: 가장 치명적
		if (cloudCover > 10) score -= (int) (cloudCover * 0.8);

		// 시정: 10km 이상이어야 좋음
		if (visibility < 5000) score -= 30; // 5km 미만이면 대폭 감점
		else if (visibility < 10000) score -= 10;

		// 달: 밝고 떠있으면 감점
		if (moonAltitude > 0 && moonFraction > 0.3) {
			score -= (int) (moonFraction * 40);
		}

		if (score < 0) score = 0;

		// 코멘트 생성
		String aiComment = getGeminiAnalysis(score, cloudCover, humidity, moonFraction);

		return new StargazingResponse(
			score,
			aiComment,
			new StargazingResponse.WeatherCondition(cloudCover, humidity),
			new StargazingResponse.AstronomyCondition(moonFraction, moonPhaseName)
		);
	}

	private String getMoonPhaseName(double phase) {
		// phase는 -180 ~ 180도
		double p = Math.abs(phase);
		if (p < 10) return "New Moon (삭)";
		if (p < 80) return "Waxing Crescent (초승달)";
		if (p < 100) return "First Quarter (상현달)";
		if (p < 170) return "Waxing Gibbous (보름달로 가는 중)";
		return "Full Moon (보름달)";
	}

	private String getGeminiAnalysis(int score, double cloud, double humidity, double moon) {
		String prompt = String.format("""
            너는 천체 관측 전문가야. 지금 관측 조건이 다음과 같아.
            - 종합 점수: %d점 (100점 만점)
            - 구름: %.1f%%
            - 습도: %.1f%%
            - 월령(달 밝기): %.2f (0은 삭, 1은 보름달)
            
            이 데이터를 바탕으로 사용자에게 별을 볼 수 있을지 없을지 '한 줄 요약'과 '전문적인 조언'을 섞어서 말해줘. (최대 2문장, 경어체 사용)
            """, score, cloud, humidity, moon);

		String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiKey;

		try {
			GeminiResponse response = restTemplate.postForObject(
				url,
				GeminiRequest.of(prompt),
				GeminiResponse.class
			);

			if (response != null) {
				return response.getText().trim();
			}
		} catch (Exception e) {
			log.error("Gemini 호출 실패: {}", e.getMessage());
		}

		return "데이터를 분석 중입니다. (AI 연결 일시 지연)";
	}
}
