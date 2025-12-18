package xyz.ncookie.stargazer.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.dto.StargazingRequest;
import xyz.ncookie.stargazer.dto.StargazingResponse;
import xyz.ncookie.stargazer.service.StargazingService;

@RestController
@RequestMapping("/api/v1/analyze")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 프론트엔드(React)에서 호출 허용
public class StargazingController {

	private final StargazingService stargazingService;

	@PostMapping
	public StargazingResponse analyzeStargazingCondition(@RequestBody StargazingRequest request) {
		return stargazingService.analyze(request);
	}
}
