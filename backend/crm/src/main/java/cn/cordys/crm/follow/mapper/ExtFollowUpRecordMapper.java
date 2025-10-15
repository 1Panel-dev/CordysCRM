package cn.cordys.crm.follow.mapper;

import cn.cordys.common.dto.BusinessDataPermission;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.dto.CustomerDataDTO;
import cn.cordys.crm.follow.dto.request.FollowUpRecordPageRequest;
import cn.cordys.crm.follow.dto.request.RecordHomePageRequest;
import cn.cordys.crm.follow.dto.response.FollowUpRecordListResponse;
import cn.cordys.crm.home.dto.request.HomeStatisticSearchWrapperRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtFollowUpRecordMapper {

    List<FollowUpRecordListResponse> selectPoolList(@Param("request") FollowUpRecordPageRequest request, @Param("userId") String userId, @Param("orgId") String orgId, @Param("resourceType") String resourceType, @Param("type") String type);


    List<FollowUpRecordListResponse> selectList(@Param("request") FollowUpRecordPageRequest request, @Param("userId") String userId, @Param("orgId") String orgId,
                                                @Param("resourceType") String resourceType, @Param("type") String type, @Param("customerData") CustomerDataDTO customerData);

    /**
     * 记录的汇总查询
     * @param request 请求参数
     * @param userId 用户ID
     * @param orgId 组织ID
     * @param clueDataPermission 线索业务数据权限
     * @param customerDataPermission 客户业务数据权限
     * @return 记录列表
     */
    List<FollowUpRecordListResponse>  selectTotalList(@Param("request") RecordHomePageRequest request, @Param("userId") String userId, @Param("orgId") String orgId,
                                                      @Param("clueDataPermission") DeptDataPermissionDTO clueDataPermission, @Param("customerDataPermission") DeptDataPermissionDTO customerDataPermission);

    FollowUpRecord selectRecord(@Param("customerId") String customerId, @Param("opportunityId") String opportunityId, @Param("clueId") String clueId, @Param("orgId") String orgId, @Param("type") String type);

    Long getNewContactCount(@Param("request") HomeStatisticSearchWrapperRequest request);
}
