package io.cordys.crm.system.mapper;

import io.cordys.common.dto.OptionDTO;

import java.util.List;
import java.util.Set;

public interface ExtOrganizationMapper {

	/**
	 * 获取所有组织ID集合
	 * @return 组织ID集合
	 */
	Set<String> selectAllOrganizationIds();

	/**
	 * 获取所有组织选项
	 * @return
	 */
	List<OptionDTO> selectOrgOption();
}
