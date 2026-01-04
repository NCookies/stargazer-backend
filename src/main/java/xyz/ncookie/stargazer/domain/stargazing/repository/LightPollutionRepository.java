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
	 * 1. WHERE MBRContains(...) : 인덱스를 사용해 주변(약 11km) 반경의 데이터만 1차로 필터링
	 * 2. ORDER BY ... : 걸러진 소수의 데이터 중에서만 정밀 거리 계산 수행
	 */
	@Query(value = """
        SELECT * FROM light_pollution lp
        WHERE MBRContains(
            ST_Buffer(ST_GeomFromText(:point, 4326), 10000), -- 0.1도 반경 (약 10km) 버퍼 생성
            lp.location
        )
        ORDER BY ST_Distance_Sphere(lp.location, ST_GeomFromText(:point, 4326)) ASC
        LIMIT 1
    """, nativeQuery = true)
	Optional<LightPollution> findNearest(@Param("point") String pointText);
}
