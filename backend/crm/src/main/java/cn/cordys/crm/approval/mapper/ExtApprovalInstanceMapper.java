package cn.cordys.crm.approval.mapper;

import org.apache.ibatis.annotations.Param;

public interface ExtApprovalInstanceMapper {

	/**
	 * 提审
	 * @param sourceTable 主业务表
	 * @param resourceId 资源ID
	 */
	void approving(@Param("sourceTable") String sourceTable, @Param("id") String resourceId);
}
