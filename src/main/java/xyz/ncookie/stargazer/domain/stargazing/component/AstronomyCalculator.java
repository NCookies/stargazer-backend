package xyz.ncookie.stargazer.domain.stargazing.component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.shredzone.commons.suncalc.MoonIllumination;
import org.shredzone.commons.suncalc.MoonPosition;
import org.shredzone.commons.suncalc.MoonTimes;
import org.shredzone.commons.suncalc.SunPosition;
import org.shredzone.commons.suncalc.SunTimes;
import org.springframework.stereotype.Component;

import xyz.ncookie.stargazer.domain.stargazing.model.RawAstronomyData;

@Component
public class AstronomyCalculator {

	// 천문 데이터 계산 (월령, 일몰, 월출 등)
	public RawAstronomyData calculate(double lat, double lon, ZonedDateTime dateTime) {

		// 월령 및 위상
		MoonIllumination moonIllum = MoonIllumination.compute().on(dateTime).execute();

		// 달 위치 (고도)
		MoonPosition moonPos = MoonPosition.compute().at(lat, lon).on(dateTime).execute();

		// 일몰 시간
		SunTimes sunTimes = SunTimes.compute().on(dateTime).at(lat, lon).execute();
		String sunset = (sunTimes.getSet() != null)
			? sunTimes.getSet().withZoneSameInstant(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("HH:mm"))
			: "--:--";

		// 월출 시간
		MoonTimes moonTimes = MoonTimes.compute().on(dateTime).at(lat, lon).execute();
		String moonrise = (moonTimes.getRise() != null)
			? moonTimes.getRise().withZoneSameInstant(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("HH:mm"))
			: "뜨지 않음";

		return new RawAstronomyData(
			moonIllum.getFraction(),
			moonIllum.getPhase(),
			moonPos.getAltitude(),
			moonrise,
			sunset
		);
	}

	// 낮/밤 판별 (시민박명 기준)
	public boolean isDaytime(double lat, double lon, ZonedDateTime time) {

		SunPosition sunPos = SunPosition.compute().at(lat, lon).on(time).execute();
		return sunPos.getAltitude() > -6.0;
	}

	// 여명(박명) 페널티 계산 로직도 여기 혹은 ScoringEngine에 위치 가능
	public double getSunAltitude(double lat, double lon, ZonedDateTime time) {

		SunPosition sunPos = SunPosition.compute().at(lat, lon).on(time).execute();
		return sunPos.getAltitude();
	}
}
