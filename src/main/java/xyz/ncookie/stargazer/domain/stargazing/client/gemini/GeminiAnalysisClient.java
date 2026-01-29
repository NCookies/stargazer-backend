package xyz.ncookie.stargazer.domain.stargazing.client.gemini;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherResponse;
import xyz.ncookie.stargazer.domain.stargazing.model.GeminiAnalysisResult;
import xyz.ncookie.stargazer.domain.stargazing.enums.MoonPhase;
import xyz.ncookie.stargazer.domain.stargazing.model.RawAstronomyData;
import xyz.ncookie.stargazer.domain.stargazing.enums.VisibilityGrade;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiAnalysisClient {

	@Value("${gemini.api.key}")
	private String geminiKey;

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public GeminiAnalysisResult getAnalysis(
		int finalScore,
		List<String> reasons,
		double lat,
		double lon,
		OpenWeatherResponse w,
		RawAstronomyData a,
		int bortleClass
	) {

		String prompt = String.format("""
			너는 천체 관측 예보 전문가야. 아래 제공된 **확정 데이터(Fact)**를 바탕으로 사용자에게 관측 조언을 해줘.
		
			[관측지 정보]
			- **광해 등급: Bortle Class %d** (정밀 지도 데이터 기반, 1~9등급)
			- 특징:
			  * Class 1~4: 별이 쏟아지는 시골/산간 지역 (관측 최적)
			  * Class 5~6: 교외 지역, 밝은 별 위주 관측 가능
			  * Class 7~9: 도심지, 행성/달 위주 관측 가능 (광해 심함)
		
			[기상 및 천문 데이터]
			- **최종 관측 점수: %d점** (기상과 광해 페널티가 이미 반영된 최종값)
			- 사유 : %s
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
			bortleClass,             // 광해 등급 (CSV 값)
			finalScore,
			reasons,
			w.clouds().all(),       // 구름
			VisibilityGrade.from(w.visibility()).getLabel(), // 시정 텍스트
			MoonPhase.calculate(a.moonPhaseDegree()), // 달 이름
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
}
