package cn.cordys.crm.system.excel;

import java.util.List;

/**
 * 业务导入后置处理函数
 * @author song-cc-rock
 */
@FunctionalInterface
public interface CustomImportAfterDoConsumer<T, BaseResourceSubField> {

    /**
     * 处理
     *
     * @param dataList      主表数据集合
     * @param fieldList     自定义字段数据集合
     * @param fieldBlobList 自定义Blob字段数据集合
     */
    void accept(List<T> dataList, List<BaseResourceSubField> fieldList, List<BaseResourceSubField> fieldBlobList);
}
