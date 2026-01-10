package xyz.ncookie.stargazer.domain.spot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ObservationSpot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String title;           // 명소 이름 (예: 강릉 안반데기)

	@Column(nullable = false)
	private String address;         // 지번/도로명 주소 (네비게이션용)

	// --- 위치 정보 ---
	@Column(nullable = false)
	private Double latitude;        // 위도

	@Column(nullable = false)
	private Double longitude;       // 경도

	// --- 관측 관련 메타데이터 ---
	@Column(length = 1000)
	private String description;     // 소개글

	private Integer lightPollutionLevel; // 광해 등급 (Bortle Scale 1~9, 낮을수록 좋음) -> 수동 입력 시 유용

	// --- 편의 시설 ---
	private Boolean isParkingAvailable; 	// 주차 가능 여부
	private Boolean isRestroomAvailable; 	// 화장실 유무
	private Boolean isCarAccess;        	// 차량 진입 가능 여부 (차박 가능 여부)

	private String thumbnailImage;  // 썸네일 이미지 URL

	public ObservationSpot(String title, Double latitude, Double longitude) {
		this.title = title;
		this.latitude = latitude;
		this.longitude = longitude;
	}
}
