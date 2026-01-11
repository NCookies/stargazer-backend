package xyz.ncookie.stargazer.domain.spot.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record ObservationSpotRequest(

	@DecimalMin(value = "-90.0", message = "위도는 -90.0 이상이어야 합니다.")
	@DecimalMax(value = "90.0", message = "위도는 90.0 이하이어야 합니다.")
	Double lat,

	@DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
	@DecimalMax(value = "180.0", message = "경도는 180.0 이하이어야 합니다.")
	Double lon,

	@DecimalMin(value = "10", message = "반경은 10km 이상이어야 합니다.")
	@DecimalMax(value = "500", message = "반경은 500km 이하이어야 합니다.")
	Integer radius
) {
}
