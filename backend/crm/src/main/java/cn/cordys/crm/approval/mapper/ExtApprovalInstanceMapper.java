package cn.cordys.crm.approval.mapper;

import org.apache.ibatis.annotations.Param;

public interface ExtApprovalInstanceMapper {

	/**
	 * 更新业务表的审批状态
	 *
	 * @param sourceTable     主业务表
	 * @param resourceId      资源ID
	 * @param approvalStatus  审批状态
	 */
	void updateApprovalStatus(@Param("sourceTable") String sourceTable, @Param("id") String resourceId,
							  @Param("approvalStatus") String approvalStatus);

	/**
	 * 查询业务表业务名称
	 * @param sourceTable
	 * @param id
	 */
	String selectBusinessName(@Param("sourceTable") String sourceTable, @Param("id") String id);

	String getResourceOwner(@Param("sourceTable")String sourceTable, @Param("id")String id);
}
