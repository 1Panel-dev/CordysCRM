package cn.cordys.crm.contract.mapper;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.crm.contract.dto.request.ContractInvoicePageRequest;
import cn.cordys.crm.contract.dto.response.ContractInvoiceGetResponse;
import cn.cordys.crm.contract.dto.response.ContractInvoiceListResponse;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ExtContractInvoiceMapper {


    List<ContractInvoiceListResponse> list(@Param("request") ContractInvoicePageRequest request, @Param("orgId") String orgId,
                                    @Param("userId") String userId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    ContractInvoiceGetResponse getDetail(@Param("id") String id);


    List<ContractInvoiceListResponse> getListByIds(@Param("ids") List<String> ids, @Param("userId") String userId, @Param("orgId") String orgId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    List<String> selectByStatusAndIds(@Param("ids") List<String> ids, @Param("approvalStatus") String approvalStatus);

    void updateStatus(@Param("id") String id, @Param("approvalStatus") String approvalStatus, @Param("userId") String userId, @Param("updateTime") long updateTime);

    BigDecimal calculateCustomerInvoiceAmount(@Param("customerId") String customerId, @Param("userId") String userId,
                                              @Param("orgId") String orgId);

    BigDecimal calculateContractInvoiceAmount(@Param("contractId") String contractId, @Param("userId") String userId,
                                              @Param("orgId") String orgId);

    BigDecimal calculateContractInvoiceValidAmount(@Param("contractId") String contractId, @Param("userId") String userId,
                                              @Param("orgId") String orgId, @Param("excludeId") String excludeId);

    Boolean hasContractInvoice(@Param("contractId") String contractId);
}
