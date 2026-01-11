package xyz.ncookie.stargazer.domain.spot.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xyz.ncookie.stargazer.domain.spot.dto.request.ObservationSpotRequest;
import xyz.ncookie.stargazer.domain.spot.dto.response.ObservationSpotResponse;
import xyz.ncookie.stargazer.domain.spot.service.ObservationSpotService;

@RestController
@RequestMapping("/api/v1/spots")
@RequiredArgsConstructor
public class ObservationSpotController {

	private final ObservationSpotService observationSpotService;

	@GetMapping
	public List<ObservationSpotResponse> getObservationSpots(
		@Valid @ModelAttribute ObservationSpotRequest request
	) {

		return observationSpotService.getObservationSpots(request);
	}

}
