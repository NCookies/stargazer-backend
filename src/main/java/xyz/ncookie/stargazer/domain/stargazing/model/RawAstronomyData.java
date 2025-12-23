package xyz.ncookie.stargazer.domain.stargazing.model;

public record RawAstronomyData(
	double moonFraction,    // 달 밝기 (0.0 ~ 1.0)
	double moonPhaseDegree, // 달 위상 각도
	double moonAltitude,    // 달 고도
	String moonRiseTime,    // 월출 시간 (HH:mm)
	String sunsetTime       // 일몰 시간 (HH:mm)
) {}
