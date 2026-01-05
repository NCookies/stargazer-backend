package xyz.ncookie.stargazer.domain.stargazing.component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.shredzone.commons.suncalc.MoonIllumination;
import org.shredzone.commons.suncalc.MoonPhase;
import org.shredzone.commons.suncalc.MoonPosition;
import org.shredzone.commons.suncalc.MoonTimes;
import org.shredzone.commons.suncalc.SunPosition;
import org.shredzone.commons.suncalc.SunTimes;
import org.springframework.stereotype.Component;

import xyz.ncookie.stargazer.domain.stargazing.model.RawAstronomyData;

@Component
public class AstronomyCalculator {

	private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

	// 천문 데이터 계산 (월령, 일몰, 월출 등)
	public RawAstronomyData calculate(double lat, double lon, ZonedDateTime dateTime) {

		// 월령 및 위상
		MoonIllumination moonIllum = MoonIllumination.compute().on(dateTime).execute();

		// 달 위치 (고도)
		MoonPosition moonPos = MoonPosition.compute().at(lat, lon).on(dateTime).execute();

		// 일출/일몰 시간 계산
		SunTimes sunTimes = SunTimes.compute().on(dateTime).at(lat, lon).execute();
		String sunrise = formatTime(sunTimes.getRise());
		String sunset = formatTime(sunTimes.getSet());

		// 월출/월몰 시간 계산
		MoonTimes moonTimes = MoonTimes.compute().on(dateTime).at(lat, lon).execute();
		String moonrise = formatTime(moonTimes.getRise());
		String moonset = formatTime(moonTimes.getSet());

		return new RawAstronomyData(
			moonIllum.getFraction(),
			moonIllum.getPhase(),
			moonPos.getAltitude(),
			sunrise,
			sunset,
			moonrise,
			moonset
		);
	}

	private String formatTime(ZonedDateTime time) {
		return (time != null) ? time.format(timeFormatter) : "--:--";
	}
}
