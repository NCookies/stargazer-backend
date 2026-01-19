package xyz.ncookie.stargazer.domain.stargazing.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.stargazing.dto.response.StargazingForecastResponse;
import xyz.ncookie.stargazer.domain.stargazing.dto.request.StargazingRequest;
import xyz.ncookie.stargazer.domain.stargazing.dto.response.StargazingAnalyzeResponse;
import xyz.ncookie.stargazer.domain.stargazing.service.StargazingService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StargazingController {

	private final StargazingService stargazingService;

	@GetMapping("/analyze")
	public StargazingAnalyzeResponse analyzeStargazingCondition(@Valid @ModelAttribute StargazingRequest request) {

		return stargazingService.getAnalyze(request);
	}

	@GetMapping("/forecast")
	public StargazingForecastResponse getForecast(@Valid @ModelAttribute StargazingRequest request) {

		return stargazingService.getForecast(request.lat(), request.lon());
	}
}
