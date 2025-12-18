package xyz.ncookie.stargazer.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.dto.StargazingForecastResponse;
import xyz.ncookie.stargazer.dto.StargazingRequest;
import xyz.ncookie.stargazer.dto.StargazingResponse;
import xyz.ncookie.stargazer.service.StargazingService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StargazingController {

	private final StargazingService stargazingService;

	@PostMapping("/analyze")
	public StargazingResponse analyzeStargazingCondition(@RequestBody StargazingRequest request) {

		return stargazingService.getAnalyze(request);
	}

	@PostMapping("/forecast") // POST /api/v1/stargazing/forecast
	public StargazingForecastResponse getForecast(@RequestBody StargazingRequest request) {

		return stargazingService.getForecast(request.lat(), request.lon());
	}
}
