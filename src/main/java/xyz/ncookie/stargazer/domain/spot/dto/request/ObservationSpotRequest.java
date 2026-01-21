package xyz.ncookie.stargazer.domain.spot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

@Schema(description = "관측지 조회 요청 DTO")
public record ObservationSpotRequest(

	@Schema(description = "위도", example = "37.5665", requiredMode = Schema.RequiredMode.REQUIRED)
	@DecimalMin(value = "-90.0", message = "위도는 -90.0 이상이어야 합니다.")
	@DecimalMax(value = "90.0", message = "위도는 90.0 이하이어야 합니다.")
	Double lat,

	@Schema(description = "경도", example = "126.9780", requiredMode = Schema.RequiredMode.REQUIRED)
	@DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
	@DecimalMax(value = "180.0", message = "경도는 180.0 이하이어야 합니다.")
	Double lon,

	@Schema(description = "검색 반경 (km)", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
	@DecimalMin(value = "10", message = "반경은 10km 이상이어야 합니다.")
	@DecimalMax(value = "500", message = "반경은 500km 이하이어야 합니다.")
	Integer radius
) {
}
