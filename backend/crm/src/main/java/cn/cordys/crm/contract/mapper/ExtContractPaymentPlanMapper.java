package cn.cordys.crm.contract.mapper;

import cn.cordys.common.dto.BatchUpdateDbParam;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.statistic.StatisticSqlMapper;
import cn.cordys.crm.contract.domain.ContractPaymentPlan;
import cn.cordys.crm.contract.dto.request.ContractPaymentPlanPageRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentPlanListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author jianxing
 */
public interface ExtContractPaymentPlanMapper extends StatisticSqlMapper {
    List<ContractPaymentPlanListResponse> list(@Param("request") ContractPaymentPlanPageRequest request,
            @Param("userId") String userId,
            @Param("orgId") String orgId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    List<ContractPaymentPlanListResponse> getListByIds(@Param("ids") List<String> ids, @Param("userId") String userId,
            @Param("orgId") String orgId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    void updatePaymentPlan(@Param("contractPaymentPlan") ContractPaymentPlan contractPaymentPlan);

    void batchUpdate(@Param("request") BatchUpdateDbParam request);
}
