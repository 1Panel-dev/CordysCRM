package io.cordys.crm.clue.mapper;

import io.cordys.crm.clue.domain.ClueCapacity;
import io.cordys.crm.customer.domain.CustomerCapacity;
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

	/**
	 * 更新容量
	 * @param capacity 容量
	 */
	void updateCapacity(@Param("capacity") ClueCapacity capacity);
}
