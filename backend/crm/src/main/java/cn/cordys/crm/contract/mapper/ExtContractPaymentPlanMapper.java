package cn.cordys.crm.contract.mapper;

import cn.cordys.crm.contract.dto.request.*;
import cn.cordys.crm.contract.dto.response.*;
import org.apache.ibatis.annotations.Param;
import cn.cordys.crm.contract.domain.ContractPaymentPlan;

import java.util.List;

/**
 *
 * @author jianxing
 * @date 2025-11-21 15:11:29
 */
public interface ExtContractPaymentPlanMapper {

    List<ContractPaymentPlanListResponse> list(@Param("request") ContractPaymentPlanPageRequest request, @Param("orgId") String orgId);

    boolean checkAddExist(@Param("contractPaymentPlan") ContractPaymentPlan contractPaymentPlan);

    boolean checkUpdateExist(@Param("contractPaymentPlan") ContractPaymentPlan ContractPaymentPlan);
}
