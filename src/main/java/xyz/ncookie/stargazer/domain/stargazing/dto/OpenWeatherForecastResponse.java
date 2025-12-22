package xyz.ncookie.stargazer.domain.stargazing.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenWeatherForecastResponse(List<Item> list) {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Item(
		Long dt,           // 타임스탬프
		Main main,
		List<Weather> weather,
		Clouds clouds,
		Integer visibility,
		String dt_txt      // "2025-05-20 21:00:00"
	) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Main(double temp, double humidity) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Weather(String description, String main) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Clouds(int all) {}
}
