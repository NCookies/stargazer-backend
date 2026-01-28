package xyz.ncookie.stargazer.domain.spot.controller;

import java.util.List;

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
import xyz.ncookie.stargazer.domain.spot.application.ObservationSpotApplicationService;
import xyz.ncookie.stargazer.domain.spot.dto.request.ObservationSpotRequest;
import xyz.ncookie.stargazer.domain.spot.dto.response.ObservationSpotResponse;

@Tag(name = "관측지", description = "별 관측지 조회 관련 API")
@RestController
@RequestMapping("/api/v1/spots")
@RequiredArgsConstructor
public class ObservationSpotController {

	private final ObservationSpotApplicationService observationSpotApplicationService;

	@Operation(
		summary = "관측지 조회",
		description = "지정된 위치와 반경 내의 별 관측지를 조회합니다. 인증 없이 사용 가능합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(schema = @Schema(implementation = ObservationSpotResponse.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 (위도/경도 범위 초과, 반경 범위 초과 등)"
		)
	})
	@GetMapping
	public List<ObservationSpotResponse> getObservationSpots(
		@Parameter(description = "관측지 조회 요청 정보 (위도, 경도, 반경)", required = true)
		@Valid @ModelAttribute ObservationSpotRequest request
	) {

		return observationSpotApplicationService.getObservationSpots(request);
	}
}
