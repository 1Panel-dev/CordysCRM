package io.cordys.crm.opportunity.mapper;

import io.cordys.common.dto.BasePageRequest;
import io.cordys.crm.opportunity.domain.OpportunityRule;
import io.cordys.crm.opportunity.dto.OpportunityRuleDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtOpportunityRuleMapper {

	/**
	 * 分页获取商机规则
	 * @param request 请求参数
	 * @param orgId 组织ID
	 * @return 商机规则集合
	 */
	List<OpportunityRuleDTO> list(@Param("request") BasePageRequest request, @Param("orgId") String orgId);

	/**
	 * 获取范围下商机关闭规则
	 * @param orgId 组织ID
	 * @return 商机关闭规则
	 */
	List<OpportunityRule> getAllRule(@Param("orgId") String orgId);
}
