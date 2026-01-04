package xyz.ncookie.stargazer.domain.stargazing.provider;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.stargazing.entity.LightPollution;
import xyz.ncookie.stargazer.domain.stargazing.repository.LightPollutionRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class LightPollutionDataProvider {

	private final LightPollutionRepository lightPollutionRepository;

	/**
	 * 특정 좌표의 Bortle 등급 조회
	 */
	public int getBortleClass(double lat, double lon) {

		// Point 객체 생성 포맷: "POINT(경도 위도)"
		String pointText = String.format("POINT(%f %f)", lat, lon);
		
		return lightPollutionRepository.findNearest(pointText)
			.map(LightPollution::getBortleClass)
			.orElse(4);	// 데이터가 없다면 중간값(4)로 처리

		// 데이터가 없다면 추후 주변 9칸을 검색하는 로직 등을 추가할 수 있음
	}
}
