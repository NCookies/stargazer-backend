package xyz.ncookie.stargazer.domain.stargazing.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record StargazingRequest(

	@DecimalMin(value = "-90.0", message = "위도는 -90.0 이상이어야 합니다.")
	@DecimalMax(value = "90.0", message = "위도는 90.0 이하이어야 합니다.")
	double lat,

	@DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
	@DecimalMax(value = "180.0", message = "경도는 180.0 이하이어야 합니다.")
	double lon,

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	LocalDate date,

	@DateTimeFormat(pattern = "HH:mm")
	LocalTime time
) {}
