package cn.cordys.crm.contract.mapper;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanPageRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 *
 * @author jianxing
 * @date 2025-11-21 15:11:29
 */
public interface ExtContractPaymentPlanMapper {
    List<ContractPaymentPlanListResponse> list(@Param("request") ContractPaymentPlanPageRequest request, @Param("userId") String userId,
                                               @Param("orgId") String orgId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    List<ContractPaymentPlanListResponse> getListByIds(@Param("ids") List<String> ids, @Param("orgId") String orgId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);
}
