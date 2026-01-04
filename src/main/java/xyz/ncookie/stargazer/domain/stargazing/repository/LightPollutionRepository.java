package xyz.ncookie.stargazer.domain.stargazing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import xyz.ncookie.stargazer.domain.stargazing.entity.LightPollution;

@Repository
public interface LightPollutionRepository extends JpaRepository<LightPollution, Long> {

	@Query(value = """
		SELECT * FROM light_pollution lp
		ORDER BY ST_Distance_Sphere(lp.location, ST_GeomFromText(:point, 4326)) ASC
        LIMIT 1
	""", nativeQuery = true)
	Optional<LightPollution> findNearest(@Param("point") String pointText);
}
