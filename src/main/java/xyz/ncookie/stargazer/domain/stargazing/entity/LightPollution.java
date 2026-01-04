package xyz.ncookie.stargazer.domain.stargazing.entity;

import org.locationtech.jts.geom.Point;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "light_pollution", indexes = {
	@Index(name = "idx_location", columnList = "location")
})
public class LightPollution {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// SRID 4326 (WGS84 - 위도/경도 좌표계) 명시
	@Column(columnDefinition = "POINT SRID 4326", nullable = false)
	private Point location;

	// 광해 등급
	@Column(nullable = false)
	private int bortleClass;

	private double radiance;

	public LightPollution(Point location, int bortleClass, double radiance) {
		this.location = location;
		this.bortleClass = bortleClass;
		this.radiance = radiance;
	}
}
