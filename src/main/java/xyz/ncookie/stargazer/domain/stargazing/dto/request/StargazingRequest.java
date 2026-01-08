package xyz.ncookie.stargazer.domain.stargazing.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record StargazingRequest(

	@NotNull(message = "위도는 필수입니다.")
	@DecimalMin(value = "-90.0", message = "위도는 -90.0 이상이어야 합니다.")
	@DecimalMax(value = "90.0", message = "위도는 90.0 이하이어야 합니다.")
	Double lat,

	@NotNull(message = "경도는 필수입니다.")
	@DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
	@DecimalMax(value = "180.0", message = "경도는 180.0 이하이어야 합니다.")
	Double lon,

	@NotNull(message = "날짜는 필수입니다.")
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	LocalDate date,

	@NotNull(message = "시간은 필수입니다.")
	@DateTimeFormat(pattern = "HH:mm")
	LocalTime time
) {}
