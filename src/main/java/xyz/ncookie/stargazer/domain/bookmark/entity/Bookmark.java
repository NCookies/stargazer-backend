package xyz.ncookie.stargazer.domain.bookmark.entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import xyz.ncookie.stargazer.domain.bookmark.exception.BookmarkErrorCode;
import xyz.ncookie.stargazer.domain.bookmark.exception.BookmarkException;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.spot.entity.ObservationSpot;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BookmarkType type;

	// 서비스에 등록된 명소일 경우 (Nullable)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "spot_id")
	private ObservationSpot spot;

	// 사용자 정의 장소일 경우 (Spot이 null일 때 사용)
	private String customName;
	private Double latitude;
	private Double longitude;
	private String address;

	@Column(nullable = false)
	private boolean isDeleted;

	@Builder
	public Bookmark(Member member, BookmarkType type, ObservationSpot spot, String customName, Double latitude, Double longitude, String address) {

		if (type == BookmarkType.SPOT && spot == null) {
			throw new BookmarkException(BookmarkErrorCode.INVALID_BOOKMARK_TYPE, "SPOT 타입은 spot 정보가 필수입니다.");
		}
		if (type == BookmarkType.CUSTOM && (latitude == null || longitude == null || address == null)) {
			throw new BookmarkException(BookmarkErrorCode.INVALID_BOOKMARK_TYPE, "CUSTOM 타입은 좌표, 주소 등의 정보가 필수입니다.");
		}

		this.member = member;
		this.type = type;
		this.spot = spot;
		this.customName = customName;
		this.latitude = latitude;
		this.longitude = longitude;
		this.address = address;
	}

	public boolean isOwner(Long memberId) {

		return Objects.equals(getMemberId(), memberId);
	}

	public void updateBookmarkCustomName(String name) {

		this.customName = name;
	}

	public void setDeleted() {
		this.isDeleted = true;
	}

	private Long getMemberId() {

		return this.member.getId();
	}
}
