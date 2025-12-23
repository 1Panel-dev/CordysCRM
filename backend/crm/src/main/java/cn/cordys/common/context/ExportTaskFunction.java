package cn.cordys.common.context;

@FunctionalInterface
public interface ExportTaskFunction {
    void apply() throws Exception;
}
