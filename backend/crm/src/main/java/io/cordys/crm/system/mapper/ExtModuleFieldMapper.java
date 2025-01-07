package io.cordys.crm.system.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtModuleFieldMapper {

	/**
	 * 根据ID集合批量删除字段
	 * @param ids ID集合
	 */
	void deleteByIds(@Param("ids") List<String> ids);
}
