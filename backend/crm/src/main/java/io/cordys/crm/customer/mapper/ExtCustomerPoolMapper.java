package io.cordys.crm.customer.mapper;

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
}
