package xyz.ncookie.stargazer.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenWeatherResponse(
	Main main,
	Clouds clouds,
	int visibility, // 가시거리 (미터)
	List<Weather> weather
) {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Main(
		double temp,
		double humidity
	) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Clouds(
		int all // 구름 양 (0~100%)
	) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Weather(
		String description, // 날씨 설명 (ex: clear sky)
		String main
	) {}
}
