package xyz.ncookie.stargazer.global.cache;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * L1(로컬) + L2(원격) 이중 캐시를 사용하는 CacheManager.
 * 지정된 캐시 이름에 대해서만 TwoLevelCache를 반환하고, 나머지는 L2만 사용.
 */
public class TwoLevelCacheManager implements CacheManager {

	private static final Set<String> L1_L2_CACHE_NAMES = Set.of("weatherForecast", "bortleZone");

	private final CacheManager l1;
	private final CacheManager l2;

	public TwoLevelCacheManager(CacheManager l1, CacheManager l2) {
		this.l1 = l1;
		this.l2 = l2;
	}

	@Override
	@org.springframework.lang.Nullable
	public Cache getCache(String name) {
		if (L1_L2_CACHE_NAMES.contains(name)) {
			return new TwoLevelCache(l1.getCache(name), l2.getCache(name));
		}
		return l2.getCache(name);
	}

	@Override
	public Collection<String> getCacheNames() {
		return Stream.concat(
			l1.getCacheNames().stream(),
			l2.getCacheNames().stream()
		).distinct().collect(Collectors.toSet());
	}
}
