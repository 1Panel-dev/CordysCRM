package cn.cordys.crm.contract.mapper;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.crm.contract.domain.ContractPaymentPlan;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanPageRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanListResponse;
import cn.cordys.crm.contract.dto.response.CustomerPaymentPlanStatisticResponse;
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

    List<ContractPaymentPlanListResponse> getListByIds(@Param("ids") List<String> ids, @Param("userId") String userId, @Param("orgId") String orgId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    CustomerPaymentPlanStatisticResponse calculateCustomerPaymentPlanStatistic(@Param("customerId") String customerId, @Param("userId") String userId, @Param("orgId") String orgId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    List<ContractPaymentPlan> selectByTimestamp(@Param("timestamp") long timestamp, @Param("timestampOld") long timestampOld, @Param("orgId") String organizationId);
}
