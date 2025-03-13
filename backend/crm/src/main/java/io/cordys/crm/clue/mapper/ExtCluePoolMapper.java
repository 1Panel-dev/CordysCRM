package io.cordys.crm.clue.mapper;

import io.cordys.common.dto.BasePageRequest;
import io.cordys.crm.clue.domain.CluePool;
import io.cordys.crm.clue.domain.CluePoolPickRule;
import io.cordys.crm.clue.domain.CluePoolRecycleRule;
import io.cordys.crm.clue.dto.CluePoolDTO;
import io.cordys.crm.customer.domain.CustomerPool;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtCluePoolMapper {

	/**
	 * 查询线索池列表
	 * @param request 请求参数
	 * @param orgId 组织ID
	 * @return 线索池列表
	 */
	List<CluePoolDTO> list(@Param("request") BasePageRequest request, @Param("orgId") String orgId);

	/**
	 * 更新线索池领取规则
	 * @param rule 领取规则
	 */
	void updatePickRule(@Param("rule") CluePoolPickRule rule);

	/**
	 * 更新线索池回收规则
	 * @param rule 回收规则
	 */
	void updateRecycleRule(@Param("rule") CluePoolRecycleRule rule);

	/**
	 * 获取公海池集合
	 * @param scopeIds 范围ID集合
	 * @param orgId 组织ID
	 * @return 公海池集合
	 */
	List<CluePool> getPoolByScopeIds(@Param("ids") List<String> scopeIds, @Param("orgId") String orgId);
}
