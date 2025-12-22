package xyz.ncookie.stargazer.domain.stargazing.dto;

public record StargazingRequest(
	double lat,          // 위도
	double lon,          // 경도
	String date,         // "2024-05-20"
	String time          // "22:00"
) {}
