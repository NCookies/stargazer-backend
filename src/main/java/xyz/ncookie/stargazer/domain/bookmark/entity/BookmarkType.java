package xyz.ncookie.stargazer.domain.bookmark.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "북마크 타입. SPOT: 서비스에 등록된 관측 명소, CUSTOM: 사용자가 직접 등록한 장소.")
public enum BookmarkType {
	CUSTOM,
	SPOT
}
