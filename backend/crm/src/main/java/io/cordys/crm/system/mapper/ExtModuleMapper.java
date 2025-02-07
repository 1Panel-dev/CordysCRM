package io.cordys.crm.system.mapper;

import org.apache.ibatis.annotations.Param;

public interface ExtModuleMapper {

	/**
	 * 模块区间上移
	 * @param start 开始
	 * @param end 结束
	 */
	void moveUpModule(@Param("start") Long start, @Param("end") Long end);

	/**
	 * 模块区间下移
	 * @param start 开始
	 * @param end 结束
	 */
	void moveDownModule(@Param("start") Long start, @Param("end") Long end);
}
