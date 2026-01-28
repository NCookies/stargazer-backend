package xyz.ncookie.stargazer.domain.stargazing.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.stargazing.application.StargazingApplicationService;
import xyz.ncookie.stargazer.domain.stargazing.dto.response.StargazingForecastResponse;
import xyz.ncookie.stargazer.domain.stargazing.dto.request.StargazingRequest;
import xyz.ncookie.stargazer.domain.stargazing.dto.response.StargazingAnalyzeResponse;

@Tag(name = "별 관측", description = "별 관측 조건 분석 및 예보 관련 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StargazingController {

	private final StargazingApplicationService stargazingApplicationService;

	@Operation(
		summary = "별 관측 조건 분석",
		description = "지정된 위치, 날짜, 시간의 별 관측 조건을 종합적으로 분석합니다. 기상 정보, 천문 정보, 광해 정보를 포함한 상세 분석 결과를 제공합니다. 인증 없이 사용 가능합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "분석 성공",
			content = @Content(schema = @Schema(implementation = StargazingAnalyzeResponse.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 (위도/경도 범위 초과, 날짜/시간 형식 오류 등)"
		),
		@ApiResponse(
			responseCode = "500",
			description = "서버 오류 (외부 API 호출 실패 등)"
		)
	})
	@GetMapping("/analyze")
	public StargazingAnalyzeResponse analyzeStargazingCondition(
		@Parameter(description = "별 관측 조건 분석 요청 정보 (위도, 경도, 날짜, 시간)", required = true)
		@Valid @ModelAttribute StargazingRequest request
	) {

		return stargazingApplicationService.analyze(request);
	}

	@Operation(
		summary = "별 관측 예보 조회",
		description = "지정된 위치의 향후 며칠간의 별 관측 예보를 조회합니다. 날짜별, 시간대별로 상세한 관측 조건 점수와 정보를 제공합니다. 인증 없이 사용 가능합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(schema = @Schema(implementation = StargazingForecastResponse.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 (위도/경도 범위 초과 등)"
		),
		@ApiResponse(
			responseCode = "500",
			description = "서버 오류 (외부 API 호출 실패 등)"
		)
	})
	@GetMapping("/forecast")
	public StargazingForecastResponse getForecast(
		@Parameter(description = "별 관측 예보 요청 정보 (위도, 경도, 날짜, 시간 - 날짜와 시간은 선택사항)", required = true)
		@Valid @ModelAttribute StargazingRequest request
	) {

		return stargazingApplicationService.getForecast(request.lat(), request.lon());
	}
}
