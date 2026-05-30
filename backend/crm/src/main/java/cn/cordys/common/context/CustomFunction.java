package cn.cordys.common.context;

@FunctionalInterface
public interface CustomFunction<T, R> {
    R apply(T t) throws InterruptedException;
}