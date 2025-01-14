package io.cordys.crm.lead.mapper;

import io.cordys.crm.lead.dto.LeadPoolDTO;
import io.cordys.crm.lead.dto.request.LeadPoolPageRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtLeadPoolMapper {

	/**
	 * 查询线索池列表
	 * @param request 请求参数
	 * @param orgId 组织ID
	 * @return 线索池列表
	 */
	List<LeadPoolDTO> list(@Param("request") LeadPoolPageRequest request, @Param("orgId") String orgId);
}
