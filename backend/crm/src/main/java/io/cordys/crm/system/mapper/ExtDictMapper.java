package io.cordys.crm.system.mapper;

import io.cordys.crm.system.domain.DictConfig;
import org.apache.ibatis.annotations.Param;

public interface ExtDictMapper {

	/**
	 * 更新字典模块配置
	 * @param config 配置
	 */
	void updateModuleConfig(@Param("config")DictConfig config);

	/**
	 * 字典区间上移
	 * @param start 开始
	 * @param end 结束
	 */
	void moveUpDict(@Param("start") Long start, @Param("end") Long end);

	/**
	 * 字典区间下移
	 * @param start 开始
	 * @param end 结束
	 */
	void moveDownDict(@Param("start") Long start, @Param("end") Long end);

	/**
	 * 获取字典模块下一个位置
	 * @param module 模块
	 * @param orgId 组织ID
	 * @return 下一个位置
	 */
	Long getNextPos(@Param("module") String module, @Param("orgId") String orgId);
}
