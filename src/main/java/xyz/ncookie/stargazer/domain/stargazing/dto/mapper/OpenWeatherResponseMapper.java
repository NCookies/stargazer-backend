package xyz.ncookie.stargazer.domain.stargazing.dto.mapper;

import org.springframework.stereotype.Component;

import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherForecastResponse;
import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherResponse;

@Component
public class OpenWeatherResponseMapper {

	public OpenWeatherResponse toWeatherResponse(OpenWeatherForecastResponse.Item item) {
		if (item == null) {
			return null; // 혹은 예외 처리
		}

		return new OpenWeatherResponse(
			new OpenWeatherResponse.Main(
				item.main().temp(),
				item.main().humidity()
			),
			new OpenWeatherResponse.Clouds(
				item.clouds().all()
			),
			// null safe 처리
			(item.visibility() != null) ? item.visibility() : 10000,
			null // Sys 정보는 예보에 없으므로 null
		);
	}
}
