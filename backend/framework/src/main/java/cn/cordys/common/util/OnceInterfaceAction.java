package cn.cordys.common.util;

/**
 * 执行单次接口 (参数版本)
 * @param <P> 参数类型
 * @author song-cc-rock
 */
@FunctionalInterface
public interface OnceInterfaceAction<P> {

	/**
	 * 执行 (参数)
	 * @param param 参数
	 */
	void execute(P param);
}
