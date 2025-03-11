package io.cordys.crm.customer.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtCustomerCapacityMapper {

	/**
	 * 获取容量
	 * @param scopeIds 范围ID集合
	 * @param organizationId 组织ID
	 * @return 容量
	 */
	Integer getCapacityByScopeIds(@Param("ids") List<String> scopeIds, @Param("orgId") String organizationId);
}
