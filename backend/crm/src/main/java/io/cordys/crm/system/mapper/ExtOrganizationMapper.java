package io.cordys.crm.system.mapper;

import java.util.Set;

public interface ExtOrganizationMapper {

	/**
	 * 获取所有组织ID集合
	 * @return 组织ID集合
	 */
	Set<String> selectAllOrganizationIds();
}
