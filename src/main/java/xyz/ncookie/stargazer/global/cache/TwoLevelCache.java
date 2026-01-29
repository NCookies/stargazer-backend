package xyz.ncookie.stargazer.global.cache;

import java.util.concurrent.Callable;

import org.springframework.cache.Cache;

/**
 * L1(로컬) + L2(원격) 이중 캐시.
 * 조회: L1 → L2, L2 히트 시 L1에 적재 후 반환.
 * 저장/삭제: L1·L2 모두 반영.
 */
public class TwoLevelCache implements Cache {

	private final Cache l1;
	private final Cache l2;

	public TwoLevelCache(Cache l1, Cache l2) {
		this.l1 = l1;
		this.l2 = l2;
	}

	@Override
	public String getName() {
		return l1.getName();
	}

	@Override
	public Object getNativeCache() {
		return this;
	}

	@Override
	public ValueWrapper get(Object key) {
		ValueWrapper v = l1.get(key);
		if (v != null) {
			return v;
		}
		v = l2.get(key);
		if (v != null) {
			l1.put(key, v.get());
			return v;
		}
		return null;
	}

	@Override
	public <T> T get(Object key, Class<T> type) {
		T v = l1.get(key, type);
		if (v != null) {
			return v;
		}
		v = l2.get(key, type);
		if (v != null) {
			l1.put(key, v);
			return v;
		}
		return null;
	}

	@Override
	public <T> T get(Object key, Callable<T> valueLoader) {
		ValueWrapper w = l1.get(key);
		if (w != null) {
			return (T) w.get();
		}
		w = l2.get(key);
		if (w != null) {
			T v = (T) w.get();
			l1.put(key, v);
			return v;
		}
		try {
			T value = valueLoader.call();
			put(key, value);
			return value;
		} catch (Exception ex) {
			throw new Cache.ValueRetrievalException(key, valueLoader, ex);
		}
	}

	@Override
	public void put(Object key, Object value) {
		l2.put(key, value);
		l1.put(key, value);
	}

	@Override
	public void evict(Object key) {
		l2.evict(key);
		l1.evict(key);
	}

	@Override
	public void clear() {
		l2.clear();
		l1.clear();
	}
}
