package xyz.ncookie.stargazer.domain.stargazing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import xyz.ncookie.stargazer.domain.stargazing.entity.LightPollution;

@Repository
public interface LightPollutionRepository extends JpaRepository<LightPollution, Long> {

	/**
	 * ST_Buffer 대신 ST_MakeEnvelope 사용
	 * 이유: 원을 그리는 것보다 사각형을 만드는 게 훨씬 빠름 (CPU 절약)
	 * 범위: 내 위치 기준 ±0.01도 (약 1km x 1km 박스)
	 */
	@Query(value = """
        SELECT * FROM light_pollution lp
        WHERE MBRContains(
            ST_SRID(
                ST_MakeEnvelope(
                    POINT(:lon - 0.01, :lat - 0.01), -- 좌측 하단 (Min X, Min Y)
                    POINT(:lon + 0.01, :lat + 0.01)  -- 우측 상단 (Max X, Max Y)
                ),
            4326),
            lp.location
        )
        ORDER BY ST_Distance_Sphere(lp.location, ST_GeomFromText(CONCAT('POINT(', :lat, ' ', :lon, ')'), 4326)) ASC
        LIMIT 1
    """, nativeQuery = true)
	Optional<LightPollution> findNearest(@Param("lat") double lat, @Param("lon") double lon);
}
