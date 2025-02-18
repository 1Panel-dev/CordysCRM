package io.cordys.crm.lead.mapper;

import io.cordys.common.pager.condition.BasePageRequest;
import io.cordys.crm.customer.domain.CustomerPoolPickRule;
import io.cordys.crm.customer.domain.CustomerPoolRecycleRule;
import io.cordys.crm.lead.dto.LeadPoolDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtLeadPoolMapper {

	/**
	 * 查询线索池列表
	 * @param request 请求参数
	 * @param orgId 组织ID
	 * @return 线索池列表
	 */
	List<LeadPoolDTO> list(@Param("request") BasePageRequest request, @Param("orgId") String orgId);

	/**
	 * 更新线索池领取规则
	 * @param rule 领取规则
	 */
	void updatePickRule(@Param("rule") CustomerPoolPickRule rule);

	/**
	 * 更新线索池回收规则
	 * @param rule 回收规则
	 */
	void updateRecycleRule(@Param("rule") CustomerPoolRecycleRule rule);
}
