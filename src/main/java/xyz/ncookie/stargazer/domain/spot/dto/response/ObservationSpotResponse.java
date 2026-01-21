package xyz.ncookie.stargazer.domain.spot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import xyz.ncookie.stargazer.domain.spot.entity.ObservationSpot;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "관측지 정보 응답 DTO")
public record ObservationSpotResponse(
	@Schema(description = "관측지 ID", example = "1")
	Long id,

	@Schema(description = "관측지 제목", example = "강원도 평창군 대관령")
	String title,
	
	@Schema(description = "주소", example = "강원도 평창군 대관령면")
	String address,

	@Schema(description = "위도", example = "37.5665")
	Double latitude,
	
	@Schema(description = "경도", example = "126.9780")
	Double longitude,

	@Schema(description = "관측지 설명", example = "별이 잘 보이는 명소입니다.")
	String description,

	@Schema(description = "보틀 등급 (1-9, 낮을수록 좋음)", example = "3")
	Integer bortleScale,

	@Schema(description = "주차 가능 여부", example = "true")
	Boolean isParkingAvailable,
	
	@Schema(description = "화장실 이용 가능 여부", example = "true")
	Boolean isRestroomAvailable,
	
	@Schema(description = "차량 접근 가능 여부", example = "true")
	Boolean isCarAccess,

	@Schema(description = "썸네일 이미지 URL", example = "https://example.com/image.jpg")
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
