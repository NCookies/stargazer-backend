package xyz.ncookie.stargazer.infra.lightpollution;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.stargazing.entity.LightPollution;
import xyz.ncookie.stargazer.domain.stargazing.repository.LightPollutionRepository;

@Component
@Slf4j
@RequiredArgsConstructor
public class LightPollutionMigrationRunner implements CommandLineRunner {

	private final LightPollutionRepository repository;
	private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

	@Override
	public void run(String @NonNull ... args) throws Exception {
		if (repository.count() > 0) {
			log.error("광해 데이터가 이미 존재합니다. 마이그레이션을 건너뜁니다.");
			return;
		}

		log.info("광해 데이터 마이그레이션 시작...");
		long startTime = System.currentTimeMillis();

		ClassPathResource resource = new ClassPathResource("light_pollution_korea.csv");
		List<LightPollution> batchList = new ArrayList<>();
		int batchSize = 1000;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			boolean isHeader = true;

			while ((line = br.readLine()) != null) {
				if (isHeader) { isHeader = false; continue; } // 헤더 스킵

				String[] parts = line.split(",");
				if (parts.length < 4) continue;

				try {
					double lat = Double.parseDouble(parts[0]);
					double lon = Double.parseDouble(parts[1]);
					double radiance = Double.parseDouble(parts[2]);
					int bortle = Integer.parseInt(parts[3]);

					Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
					batchList.add(new LightPollution(point, bortle, radiance));

					if (batchList.size() >= batchSize) {
						repository.saveAll(batchList);
						batchList.clear();
					}
				} catch (Exception e) {
					log.error("Skipping line: " + line);
				}

			}
		}

		if (!batchList.isEmpty()) {
			repository.saveAll(batchList);
		}

		long endTime = System.currentTimeMillis();
		log.info("광해 데이터 DB 이관 완료! (소요시간: {}ms)%n", (endTime - startTime));
	}
}
