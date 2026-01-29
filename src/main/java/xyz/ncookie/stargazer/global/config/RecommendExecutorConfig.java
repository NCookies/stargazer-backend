package xyz.ncookie.stargazer.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 추천 북마크 API 병렬 호출용 Executor 설정
 * - 여러 북마크에 대한 날씨 API를 동시에 호출할 때 사용
 */
@Configuration
public class RecommendExecutorConfig {

	private static final int RECOMMEND_PARALLEL_SIZE = 10;

	@Bean
	@Qualifier("recommendTaskExecutor")
	public Executor recommendTaskExecutor() {
		AtomicInteger counter = new AtomicInteger(0);
		return Executors.newFixedThreadPool(
			RECOMMEND_PARALLEL_SIZE,
			r -> {
				Thread t = new Thread(r, "recommend-weather-" + counter.incrementAndGet());
				t.setDaemon(false);
				return t;
			}
		);
	}
}
