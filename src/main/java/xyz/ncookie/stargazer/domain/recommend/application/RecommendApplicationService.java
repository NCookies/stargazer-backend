package xyz.ncookie.stargazer.domain.recommend.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.shredzone.commons.suncalc.SunPosition;
import org.shredzone.commons.suncalc.SunTimes;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.bookmark.entity.Bookmark;
import xyz.ncookie.stargazer.domain.bookmark.repository.BookmarkRepository;
import xyz.ncookie.stargazer.domain.stargazing.domain.OpenWeatherRateLimitService;
import xyz.ncookie.stargazer.domain.recommend.dto.response.RecommendedBookmarkItemResponse;
import xyz.ncookie.stargazer.domain.recommend.dto.response.RecommendedBookmarkResponse;
import xyz.ncookie.stargazer.domain.recommend.model.BookmarkScore;
import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherForecastResponse;
import xyz.ncookie.stargazer.domain.stargazing.client.openweather.OpenWeatherMapClient;
import xyz.ncookie.stargazer.domain.stargazing.component.AstronomyCalculator;
import xyz.ncookie.stargazer.domain.stargazing.domain.StargazingDomainService;
import xyz.ncookie.stargazer.domain.stargazing.dto.mapper.OpenWeatherResponseMapper;
import xyz.ncookie.stargazer.domain.stargazing.model.HourlyForecastData;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendApplicationService {

	private final StargazingDomainService stargazingDomainService;
	private final OpenWeatherRateLimitService rateLimitService;

	private final BookmarkRepository bookmarkRepository;
	private final OpenWeatherMapClient weatherMapClient;
	private final OpenWeatherResponseMapper openWeatherResponseMapper;
	private final AstronomyCalculator astronomyCalculator;

	@Qualifier("recommendTaskExecutor")
	private final Executor recommendTaskExecutor;

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
	private static final int RECOMMEND_COUNT = 5;

	/**
	 * 오늘 관측이 적합한 북마크 장소 TOP 5 추천
	 */
	@Transactional(readOnly = true)
	public RecommendedBookmarkResponse getTodayRecommendedBookmarks(Long memberId) {

		log.info("오늘의 추천 북마크 조회 시작! memberId={}", memberId);

		List<Bookmark> bookmarks = bookmarkRepository.findAllByMember_Id(memberId);
		int totalCount = bookmarks.size();

		if (bookmarks.isEmpty()) {
			return new RecommendedBookmarkResponse(null, 0, 0, false);
		}

		ZonedDateTime now = ZonedDateTime.now(SEOUL_ZONE);
		LocalDate today = now.toLocalDate();
		LocalDate tomorrow = today.plusDays(1);

		// 북마크별로 날씨 API 호출 + 점수 계산을 병렬 수행 (데이터 누락 없이 전부 대기)
		List<CompletableFuture<BookmarkScore>> futures = bookmarks.stream()
			.filter(bookmark -> {
				if (bookmark.getLatitude() == null || bookmark.getLongitude() == null) {
					log.warn("Bookmark {} has no coordinates, skipping", bookmark.getId());
					return false;
				}
				return true;
			})
			.map(bookmark -> CompletableFuture.supplyAsync(() -> {
				long startMs = System.currentTimeMillis();

				if (!rateLimitService.tryConsume()) {
					// 토큰 부족 시: 로그 남기고 null 반환 (Skip)
					log.warn("Rate limit exceeded for bookmark {}. Skipped.", bookmark.getId());
					return null;
				}

				try {
					Double lat = bookmark.getLatitude();
					Double lon = bookmark.getLongitude();
					OpenWeatherForecastResponse forecastData = weatherMapClient.fetchForecastApi(lat, lon);
					if (forecastData == null || forecastData.list() == null) {
						log.warn("Failed to fetch forecast for bookmark {}", bookmark.getId());
						return null;
					}
					BookmarkScore score = calculateBestScore(bookmark, lat, lon, today, tomorrow, forecastData);
					log.debug("Bookmark {} ({}): {} ms", bookmark.getId(), bookmark.getName(),
						System.currentTimeMillis() - startMs);
					return score;
				} catch (Exception e) {
					log.warn("Bookmark {} forecast error after {} ms: {}", bookmark.getId(),
						System.currentTimeMillis() - startMs, e.getMessage());
					return null;
				}
			}, recommendTaskExecutor))
			.toList();

		List<BookmarkScore> bookmarkScores = futures.stream()
			.map(CompletableFuture::join)
			.filter(Objects::nonNull)
			.toList();

		// 점수 기준으로 정렬하여 TOP 5 추출
		List<RecommendedBookmarkItemResponse> recommend = bookmarkScores.stream()
			.sorted(Comparator.comparing(BookmarkScore::score).reversed())
			.limit(RECOMMEND_COUNT)
			.map(BookmarkScore::toResponse)
			.toList();

		int analyzedCount = bookmarkScores.size();
		boolean isPartial = analyzedCount < totalCount;

		return new RecommendedBookmarkResponse(
			recommend,
			totalCount,
			analyzedCount,
			isPartial
		);
	}

	/**
	 * 오늘 저녁부터 내일 일출 전까지의 최고 점수 계산
	 */
	private BookmarkScore calculateBestScore(
		Bookmark bookmark,
		double lat,
		double lon,
		LocalDate today,
		LocalDate tomorrow,
		OpenWeatherForecastResponse forecastData
	) {

		// 오늘 일몰 시간 계산
		ZonedDateTime todayStart = ZonedDateTime.of(today, LocalTime.MIN, SEOUL_ZONE);
		SunTimes todaySunTimes = astronomyCalculator.calculateSunTimes(lat, lon, todayStart);
		ZonedDateTime sunset = todaySunTimes.getSet();

		// 내일 일출 시간 계산
		ZonedDateTime tomorrowStart = ZonedDateTime.of(tomorrow, LocalTime.MIN, SEOUL_ZONE);
		SunTimes tomorrowSunTimes = astronomyCalculator.calculateSunTimes(lat, lon, tomorrowStart);
		ZonedDateTime sunrise = tomorrowSunTimes.getRise();

		if (sunset == null || sunrise == null) {
			log.warn("Failed to calculate sunset/sunrise for bookmark {}", bookmark.getId());
			return null;
		}

		// 예보 데이터 중 오늘 저녁부터 내일 일출 전까지 필터링
		int bestScore = -1;
		HourlyForecastData bestForecast = null;

		for (OpenWeatherForecastResponse.Item item : forecastData.list()) {
			ZonedDateTime itemTime = ZonedDateTime.ofInstant(
				java.time.Instant.ofEpochSecond(item.dt()), SEOUL_ZONE
			);

			// 시간대 필터링: 오늘 일몰 이후 ~ 내일 일출 이전
			if (itemTime.isBefore(sunset) || itemTime.isAfter(sunrise)) {
				continue;
			}

			// 야간 시간대만 (태양 고도 -6도 이하)
			SunPosition sunPos = SunPosition.compute().at(lat, lon).on(itemTime).execute();
			if (sunPos.getAltitude() > -6.0) {
				continue;
			}

			// 날씨 데이터 변환 및 점수 계산
			var weatherResponse = openWeatherResponseMapper.toWeatherResponse(item);
			HourlyForecastData forecast = stargazingDomainService.calculateHourlyForecast(
				lat, lon, itemTime, weatherResponse
			);

			// 최고 점수 업데이트
			if (forecast.score() > bestScore) {
				bestScore = forecast.score();
				bestForecast = forecast;
			}
		}

		if (bestForecast == null) {
			return null;
		}

		return new BookmarkScore(
			bookmark,
			bestScore,
			bestForecast.reasons(),
			bestForecast.starGrade(),
			bestForecast.cloudCover(),
			bestForecast.moonPhase(),
			bestForecast.dateTime().format(TIME_FORMATTER)
		);
	}
}
