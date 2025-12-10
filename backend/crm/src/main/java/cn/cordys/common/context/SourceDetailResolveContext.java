package cn.cordys.common.context;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据源详情解析上下文
 *
 * @author song-cc-rock
 */
public class SourceDetailResolveContext {

	private static final ThreadLocal<Map<String, Map<String, Object>>> CONTEXT =
			ThreadLocal.withInitial(HashMap::new);
	private static final ThreadLocal<Integer> DEPTH =
			ThreadLocal.withInitial(() -> 0);

	public static Map<String, Map<String, Object>> getSourceMap() {
		return CONTEXT.get();
	}

	public static boolean contains(String sourceId) {
		return CONTEXT.get().containsKey(sourceId);
	}

	public static void putPlaceholder(String sourceId) {
		CONTEXT.get().putIfAbsent(sourceId, new HashMap<>(8));
	}

	public static void put(String sourceId, Map<String, Object> detail) {
		CONTEXT.get().put(sourceId, detail);
	}

	public static void start() {
		DEPTH.set(DEPTH.get() + 1);
	}

	public static void end() {
		int depth = DEPTH.get() - 1;
		if (depth <= 0) {
			clear();
			DEPTH.remove();
		} else {
			DEPTH.set(depth);
		}
	}

	public static void clear() {
		CONTEXT.remove();
	}

	public static void remove(String sourceId) {
		CONTEXT.get().remove(sourceId);
	}

	private SourceDetailResolveContext() {

	}
}
