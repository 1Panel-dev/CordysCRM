package io.cordys.crm.system.excel;

import io.cordys.common.domain.BaseResourceField;

import java.util.List;

/**
 * 业务导入后置处理函数
 */
@FunctionalInterface
public interface CustomImportAfterDoConsumer<T, BaseResourceField> {

	/**
	 * 处理
	 * @param dataList 主表数据集合
	 * @param fieldList 自定义字段数据集合
	 * @param fieldBlobList 自定义Blob字段数据集合
	 */
	void accept(List<T> dataList, List<BaseResourceField> fieldList, List<BaseResourceField> fieldBlobList);
}
