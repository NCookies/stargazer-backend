package xyz.ncookie.stargazer.domain.spot.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.spot.dto.response.ObservationSpotResponse;
import xyz.ncookie.stargazer.domain.spot.repository.ObservationRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class ObservationSpotService {

	private final ObservationRepository observationRepository;

	public List<ObservationSpotResponse> getObservationSpots() {

		return observationRepository.findAll()
			.stream().map(ObservationSpotResponse::from)
			.toList();
	}
}
