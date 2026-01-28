package xyz.ncookie.stargazer.domain.spot.domain;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.spot.entity.ObservationSpot;
import xyz.ncookie.stargazer.domain.spot.exception.ObservationSpotErrorCode;
import xyz.ncookie.stargazer.domain.spot.exception.ObservationSpotException;
import xyz.ncookie.stargazer.domain.spot.repository.ObservationRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class ObservationSpotDomainService {

	private final ObservationRepository observationRepository;

	public ObservationSpot findById(Long spotId) {

		return observationRepository.findById(spotId)
			.orElseThrow(() -> new ObservationSpotException(ObservationSpotErrorCode.SPOT_NOT_FOUND, spotId.toString()));
	}
}
