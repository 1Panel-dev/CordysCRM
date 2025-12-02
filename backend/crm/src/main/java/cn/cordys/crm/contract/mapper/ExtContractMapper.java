package cn.cordys.crm.contract.mapper;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.crm.contract.dto.request.ContractPageRequest;
import cn.cordys.crm.contract.dto.response.ContractListResponse;
import cn.cordys.crm.contract.dto.response.ContractResponse;
import cn.cordys.crm.contract.dto.response.CustomerContractStatisticResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtContractMapper {


    List<ContractListResponse> list(@Param("request") ContractPageRequest request, @Param("orgId") String orgId,
                                    @Param("userId") String userId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    ContractResponse getDetail(@Param("id") String id);

    List<ContractListResponse> getListByIds(@Param("ids") List<String> ids, @Param("orgId") String orgId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    CustomerContractStatisticResponse calculateContractStatisticByCustomerId(@Param("customerId")  String customerId, @Param("userId")  String userId, @Param("orgId") String orgId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);
}
