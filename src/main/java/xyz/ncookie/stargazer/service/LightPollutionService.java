package xyz.ncookie.stargazer.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LightPollutionService {

	// 메모리 검색을 위한 Map (Key: "lat_idx,lon_idx", Value: BortleClass)
	// 0.01도(약 1.1km) 단위로 격자를 나눔
	private final Map<String, Integer> lightPollutionMap = new HashMap<>();
	private static final double GRID_SIZE = 0.01; // 정밀도 조절 (0.01 = 1km)

	@PostConstruct
	public void loadData() {
		try {
			log.info("광해 데이터(CSV) 로딩 시작...");
			ClassPathResource resource = new ClassPathResource("light_pollution_korea.csv");

			// 파일이 없으면 더미 데이터라도 로드 (에러 방지)
			if (!resource.exists()) {
				log.warn("광해 데이터 파일이 없습니다. 기본값으로 동작합니다.");
				return;
			}

			try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				boolean isHeader = true;
				while ((line = br.readLine()) != null) {
					if (isHeader) { isHeader = false; continue; } // 헤더 스킵

					String[] parts = line.split(",");
					if (parts.length < 3) continue;

					double lat = Double.parseDouble(parts[0]);
					double lon = Double.parseDouble(parts[1]);
					int bortle = Integer.parseInt(parts[2]);

					// 좌표를 격자 키로 변환 (예: 37.573 -> "3757")
					String key = generateKey(lat, lon);
					lightPollutionMap.put(key, bortle);
				}
			}
			log.info("광해 데이터 로딩 완료. (총 {}개 지점)", lightPollutionMap.size());
		} catch (Exception e) {
			log.error("광해 데이터 로딩 중 에러 발생", e);
		}
	}

	/**
	 * 특정 좌표의 Bortle 등급 조회
	 * 데이터가 없으면 '주변 탐색'을 하거나 기본값 반환
	 */
	public int getBortleClass(double lat, double lon) {
		String key = generateKey(lat, lon);

		// 1. 정확히 일치하는 격자가 있으면 반환
		if (lightPollutionMap.containsKey(key)) {
			return lightPollutionMap.get(key);
		}

		// 2. 데이터가 없다면? (바다, 혹은 데이터 누락 지역)
		// -> 안전하게 4 (시골) 또는 주소 기반 로직(fallback) 호출 필요
		// 여기서는 간단히 주변 9칸을 검색하는 로직을 추가할 수 있음
		return 4; // 기본값 (데이터 없을 때)
	}

	// 좌표 -> 격자 Key 변환 (소수점 2자리 버림)
	// 37.1234 -> 3712
	private String generateKey(double lat, double lon) {
		long latIdx = Math.round(lat / GRID_SIZE);
		long lonIdx = Math.round(lon / GRID_SIZE);
		return latIdx + "," + lonIdx;
	}
}
