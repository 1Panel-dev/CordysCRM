package cn.cordys.crm.customer.mapper;

import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.crm.customer.domain.CustomerPool;
import cn.cordys.crm.customer.domain.CustomerPoolPickRule;
import cn.cordys.crm.customer.domain.CustomerPoolRecycleRule;
import cn.cordys.crm.customer.dto.CustomerPoolDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtCustomerPoolMapper {

	/**
	 * 分页获取公海池
	 * @param request 请求参数
	 * @param orgId 组织ID
	 * @return 公海池列表
	 */
	List<CustomerPoolDTO> list(@Param("request") BasePageRequest request, @Param("orgId") String orgId);

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

	/**
	 * 获取公海池集合
	 * @param orgId 组织ID
	 * @return 公海池集合
	 */
	List<CustomerPool> getAllPool(@Param("orgId") String orgId);

	/**
	 * 获取指定范围下的公海池
	 * @param scopeIds 范围ID集合
	 * @param orgId 组织ID
	 * @return 公海池集合
	 */
	List<CustomerPool> getPoolByScopeIds(@Param("ids") List<String> scopeIds, @Param("orgId") String orgId);
}
