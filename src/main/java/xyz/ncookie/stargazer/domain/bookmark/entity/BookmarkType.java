package xyz.ncookie.stargazer.domain.bookmark.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
	description = "북마크 타입",
	allowableValues = {"CUSTOM", "SPOT"}
)
public enum BookmarkType {

	@Schema(description = "사용자 정의 위치 — 직접 입력한 좌표·주소로 저장한 북마크")
	CUSTOM,

	@Schema(description = "서비스에 등록된 관측지 — spotId로 연결된 명소 북마크")
	SPOT
}
