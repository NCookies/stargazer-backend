package xyz.ncookie.stargazer.domain.spot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.ncookie.stargazer.domain.spot.entity.ObservationSpot;

public interface ObservationRepository extends JpaRepository<ObservationSpot, Long> {

	// 특정 좌표(centerLon, centerLat)로부터 radius(미터 단위) 이내의 명소를 찾음
	@Query(value = "SELECT * FROM observation_spot s " +
		"WHERE ST_Distance_Sphere(POINT(s.longitude, s.latitude), POINT(:centerLon, :centerLat)) <= :radius " +
		"ORDER BY ST_Distance_Sphere(POINT(s.longitude, s.latitude), POINT(:centerLon, :centerLat))",
		nativeQuery = true)
	List<ObservationSpot> findSpotsWithinRadius(@Param("centerLat") double centerLat,
		@Param("centerLon") double centerLon,
		@Param("radius") double radiusInMeters);
}
