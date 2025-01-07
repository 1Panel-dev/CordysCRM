package io.cordys.crm.system.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtModuleFieldOptionMapper {

	/**
	 * 根据字段ID集合批量删除字段选项
	 * @param ids 字段ID集合
	 */
	void deleteByFieldIds(@Param("ids") List<String> ids);
}
