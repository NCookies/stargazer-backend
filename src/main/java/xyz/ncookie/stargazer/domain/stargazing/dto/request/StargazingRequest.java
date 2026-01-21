package xyz.ncookie.stargazer.domain.stargazing.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Schema(description = "별 관측 조건 분석 요청 DTO")
public record StargazingRequest(

	@Schema(description = "위도", example = "37.5665", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "위도는 필수입니다.")
	@DecimalMin(value = "-90.0", message = "위도는 -90.0 이상이어야 합니다.")
	@DecimalMax(value = "90.0", message = "위도는 90.0 이하이어야 합니다.")
	Double lat,

	@Schema(description = "경도", example = "126.9780", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "경도는 필수입니다.")
	@DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
	@DecimalMax(value = "180.0", message = "경도는 180.0 이하이어야 합니다.")
	Double lon,

	@Schema(description = "관측 날짜 (yyyy-MM-dd 형식)", example = "2025-12-25", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "날짜는 필수입니다.")
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	LocalDate date,

	@Schema(description = "관측 시간 (HH:mm 형식)", example = "22:00", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "시간은 필수입니다.")
	@DateTimeFormat(pattern = "HH:mm")
	LocalTime time
) {}
