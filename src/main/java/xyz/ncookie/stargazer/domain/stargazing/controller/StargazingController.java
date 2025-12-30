package xyz.ncookie.stargazer.domain.stargazing.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.stargazing.dto.response.StargazingForecastResponse;
import xyz.ncookie.stargazer.domain.stargazing.dto.request.StargazingRequest;
import xyz.ncookie.stargazer.domain.stargazing.dto.response.StargazingAnalyzeResponse;
import xyz.ncookie.stargazer.domain.stargazing.service.StargazingService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StargazingController {

	private final StargazingService stargazingService;

	@PostMapping("/analyze")
	public StargazingAnalyzeResponse analyzeStargazingCondition(@RequestBody StargazingRequest request) {

		return stargazingService.getAnalyze(request);
	}

	@PostMapping("/forecast") // POST /api/v1/stargazing/forecast
	public StargazingForecastResponse getForecast(@RequestBody StargazingRequest request) {

		return stargazingService.getForecast(request.lat(), request.lon());
	}
}
