package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.domain.DictConfig;
import org.apache.ibatis.annotations.Param;

public interface ExtDictMapper {

	/**
	 * 更新字典模块配置
	 * @param config 配置
	 */
	void updateModuleConfig(@Param("config") DictConfig config);

	/**
	 * 字典区间上移
	 * @param start 开始
	 * @param end 结束
	 * @param module 模块
	 * @param orgId 组织ID
	 */
	void moveUpDict(@Param("start") Long start, @Param("end") Long end, @Param("module") String module, @Param("orgId") String orgId);

	/**
	 * 字典区间下移
	 * @param start 开始
	 * @param end 结束
	 * @param module 模块
	 * @param orgId 组织ID
	 */
	void moveDownDict(@Param("start") Long start, @Param("end") Long end, @Param("module") String module, @Param("orgId") String orgId);

	/**
	 * 获取字典模块下一个位置
	 * @param module 模块
	 * @param orgId 组织ID
	 * @return 下一个位置
	 */
	Long getNextPos(@Param("module") String module, @Param("orgId") String orgId);

	/**
	 * 更新字典位置
	 * @param id 字典ID
	 * @param pos 位置
	 * @param currentUser 当前用户
	 */
	void updateDictPos(@Param("id") String id, @Param("pos") Long pos, @Param("currentUser") String currentUser);
}
