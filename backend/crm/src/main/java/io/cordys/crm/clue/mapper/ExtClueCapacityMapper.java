package io.cordys.crm.clue.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtClueCapacityMapper {

	/**
	 * 获取容量
	 * @param scopeIds 范围ID集合
	 * @param organizationId 组织ID
	 * @return 容量
	 */
	Integer getCapacityByScopeIds(@Param("ids") List<String> scopeIds, @Param("orgId") String organizationId);
}
