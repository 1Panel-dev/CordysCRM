package cn.cordys.crm.follow.mapper;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.crm.customer.dto.request.CustomerMergeRequest;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.dto.CustomerDataDTO;
import cn.cordys.crm.follow.dto.request.FollowUpPlanPageRequest;
import cn.cordys.crm.follow.dto.request.PlanHomePageRequest;
import cn.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import cn.cordys.crm.home.dto.request.HomeStatisticSearchWrapperRequest;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ExtFollowUpPlanMapper {

  List<FollowUpPlanListResponse> selectList(
      @Param("request") FollowUpPlanPageRequest request,
      @Param("userId") String userId,
      @Param("orgId") String orgId,
      @Param("resourceType") String resourceType,
      @Param("type") String type,
      @Param("customerData") CustomerDataDTO customerData,
      @Param("resourceTypeList") List<String> resourceTypeList);

  /**
   * 计划的汇总查询
   *
   * @param request 请求参数
   * @param userId 用户ID
   * @param orgId 组织ID
   * @param clueDataPermission 线索数据权限
   * @param customerDataPermission 客户数据权限
   * @return 记录列表
   */
  List<FollowUpPlanListResponse> selectTotalList(
      @Param("request") PlanHomePageRequest request,
      @Param("userId") String userId,
      @Param("orgId") String orgId,
      @Param("clueDataPermission") DeptDataPermissionDTO clueDataPermission,
      @Param("customerDataPermission") DeptDataPermissionDTO customerDataPermission);

  List<FollowUpPlan> selectPlanByTimestamp(@Param("timestamp") long timestamp);

  Long getNewFollowUpPlan(@Param("request") HomeStatisticSearchWrapperRequest request);

  /**
   * 批量合并客户(商机)记录
   *
   * @param request 请求参数
   * @param userId 用户ID
   * @param orgId 组织ID
   */
  void batchMerge(
      @Param("request") CustomerMergeRequest request,
      @Param("userId") String userId,
      @Param("orgId") String orgId);

  /**
   * 获取待合并的计划列表
   *
   * @param request 请求参数
   * @param orgId 组织ID
   * @return 计划列表
   */
  List<FollowUpPlan> getMergePlanList(
      @Param("request") CustomerMergeRequest request, @Param("orgId") String orgId);
}
