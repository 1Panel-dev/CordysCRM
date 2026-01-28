package cn.cordys.crm.contract.mapper;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordPageRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentRecordResponse;
import cn.cordys.crm.contract.dto.response.CustomerPaymentRecordStatisticResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * @author song-cc-rock
 */
public interface ExtContractPaymentRecordMapper {

  /**
   * 合同回款记录列表
   *
   * @param request 请求参数
   * @param currentUser 当前用户
   * @param currentOrg 当前组织
   * @param deptDataPermission 数据权限
   * @return 回款记录列表
   */
  List<ContractPaymentRecordResponse> list(
      @Param("request") ContractPaymentRecordPageRequest request,
      @Param("currentUser") String currentUser,
      @Param("currentOrg") String currentOrg,
      @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

  /**
   * 通过ID集合获取回款记录
   *
   * @param ids ID集合
   * @param currentUser 当前用户
   * @param currentOrg 当前组织
   * @param deptDataPermission 数据权限
   * @return 回款记录列表
   */
  List<ContractPaymentRecordResponse> getListByIds(
      @Param("ids") List<String> ids,
      @Param("currentUser") String currentUser,
      @Param("currentOrg") String currentOrg,
      @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

  /**
   * 汇总客户回款记录金额
   *
   * @param customerId 客户ID
   * @param userId 用户ID
   * @param orgId 组织ID
   * @param deptDataPermission 数据权限
   * @return 汇总结果
   */
  CustomerPaymentRecordStatisticResponse sumCustomerRecordAmount(
      @Param("customerId") String customerId,
      @Param("userId") String userId,
      @Param("orgId") String orgId,
      @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);
}
