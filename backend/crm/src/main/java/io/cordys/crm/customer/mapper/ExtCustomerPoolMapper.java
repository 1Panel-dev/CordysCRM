package io.cordys.crm.customer.mapper;

import io.cordys.crm.customer.domain.CustomerPoolPickRule;
import io.cordys.crm.customer.domain.CustomerPoolRecycleRule;
import io.cordys.crm.customer.dto.CustomerPoolDTO;
import io.cordys.crm.customer.dto.request.CustomerPoolPageRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtCustomerPoolMapper {

	/**
	 * 分页获取公海池
	 * @param request 请求参数
	 * @param orgId 组织ID
	 * @return 公海池列表
	 */
	List<CustomerPoolDTO> list(@Param("request") CustomerPoolPageRequest request, @Param("orgId") String orgId);

	/**
	 * 更新公海池领取规则
	 * @param rule 领取规则
	 */
	void updatePickRule(@Param("rule") CustomerPoolPickRule rule);

	/**
	 * 更新公海池回收规则
	 * @param rule 回收规则
	 */
	void updateRecycleRule(@Param("rule") CustomerPoolRecycleRule rule);
}
