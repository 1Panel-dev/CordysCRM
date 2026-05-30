package cn.cordys.mybatis.lambda;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 支持序列化的 Function
 */
@FunctionalInterface
public interface XFunction<T, R> extends Function<T, R>, Serializable {
}