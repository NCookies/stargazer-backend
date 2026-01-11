package xyz.ncookie.stargazer.domain.spot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import xyz.ncookie.stargazer.domain.spot.entity.ObservationSpot;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ObservationSpotResponse(
	Long id,

	String title,
	String address,

	Double latitude,
	Double longitude,

	String description,

	Integer bortleScale,

	Boolean isParkingAvailable,
	Boolean isRestroomAvailable,
	Boolean isCarAccess,

	String thumbnailImage
) {

	public static ObservationSpotResponse from(ObservationSpot spot) {
		return ObservationSpotResponse.builder()
			.id(spot.getId())
			.title(spot.getTitle())
			.address(spot.getAddress())
			.latitude(spot.getLatitude())
			.longitude(spot.getLongitude())
			.description(spot.getDescription())
			.bortleScale(spot.getLightPollutionLevel())
			.isParkingAvailable(spot.getIsParkingAvailable())
			.isRestroomAvailable(spot.getIsRestroomAvailable())
			.isCarAccess(spot.getIsCarAccess())
			.thumbnailImage(spot.getThumbnailImage())
			.build();
	}
}
