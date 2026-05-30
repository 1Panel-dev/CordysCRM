package cn.cordys.common.util;

/**
 * 执行单次接口
 * @author song-cc-rock
 */
@FunctionalInterface
public interface OnceInterface {

    /**
     * 执行
     */
    void execute();
}
