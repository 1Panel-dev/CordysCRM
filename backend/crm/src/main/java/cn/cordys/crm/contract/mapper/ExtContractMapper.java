package cn.cordys.crm.contract.mapper;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.dto.request.ContractPageRequest;
import cn.cordys.crm.contract.dto.response.ContractGetResponse;
import cn.cordys.crm.contract.dto.response.ContractListResponse;
import cn.cordys.crm.contract.dto.response.CustomerContractStatisticResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ExtContractMapper {

  List<ContractListResponse> list(
      @Param("request") ContractPageRequest request,
      @Param("orgId") String orgId,
      @Param("userId") String userId,
      @Param("dataPermission") DeptDataPermissionDTO deptDataPermission,
      @Param("source") boolean source);

  ContractGetResponse getDetail(@Param("id") String id);

  List<ContractListResponse> getListByIds(
      @Param("ids") List<String> ids,
      @Param("userId") String userId,
      @Param("orgId") String orgId,
      @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

  CustomerContractStatisticResponse calculateContractStatisticByCustomerId(
      @Param("customerId") String customerId,
      @Param("userId") String userId,
      @Param("orgId") String orgId,
      @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

  List<String> selectByStatusAndIds(
      @Param("ids") List<String> ids, @Param("approvalStatus") String approvalStatus);

  void updateStatus(
      @Param("id") String id,
      @Param("approvalStatus") String approvalStatus,
      @Param("userId") String userId,
      @Param("updateTime") long updateTime);

  void updateStage(
      @Param("id") String id,
      @Param("stage") String stage,
      @Param("userId") String userId,
      @Param("updateTime") long updateTime);

  List<Contract> selectByTimestamp(
      @Param("organizationId") String organizationId,
      @Param("timestampOld") long timestampOld,
      @Param("timestamp") long timestamp);
}
