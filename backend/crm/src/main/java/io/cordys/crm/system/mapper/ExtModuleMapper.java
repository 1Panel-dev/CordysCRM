package io.cordys.crm.system.mapper;

import org.apache.ibatis.annotations.Param;

public interface ExtModuleMapper {

	/**
	 * 区间模块上移
	 * @param start 开始
	 * @param end 结束
	 */
	void moveUpModule(@Param("start") Long start, @Param("end") Long end);

	/**
	 * 区间模块下移
	 * @param start 开始
	 * @param end 结束
	 */
	void moveDownModule(@Param("start") Long start, @Param("end") Long end);
}
