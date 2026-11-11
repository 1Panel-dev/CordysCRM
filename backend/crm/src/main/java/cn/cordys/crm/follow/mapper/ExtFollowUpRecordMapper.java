package cn.cordys.crm.follow.mapper;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.statistic.StatisticSqlMapper;
import cn.cordys.crm.customer.dto.request.CustomerMergeRequest;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.dto.request.FollowUpRecordPageRequest;
import cn.cordys.crm.follow.dto.request.RecordHomePageRequest;
import cn.cordys.crm.follow.dto.response.FollowUpRecordListResponse;
import cn.cordys.crm.home.dto.request.HomeStatisticSearchWrapperRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtFollowUpRecordMapper extends StatisticSqlMapper {

    int updateCommentCount(@Param("id") String id, @Param("orgId") String orgId,
            @Param("commentCount") long commentCount);

    List<FollowUpRecordListResponse> selectPoolList(@Param("request") FollowUpRecordPageRequest request,
            @Param("userId") String userId, @Param("orgId") String orgId, @Param("resourceType") String resourceType,
            @Param("type") String type);

    List<FollowUpRecordListResponse> selectList(@Param("request") FollowUpRecordPageRequest request,
            @Param("userId") String userId, @Param("orgId") String orgId,
            @Param("resourceType") String resourceType, @Param("type") String type);

    /**
     * 记录的汇总查询
     *
     * @param request                请求参数
     * @param userId                 用户ID
     * @param orgId                  组织ID
     * @param dataPermission 跟进记录数据权限
     * @return 记录列表
     */
    List<FollowUpRecordListResponse> selectTotalList(@Param("request") RecordHomePageRequest request,
            @Param("userId") String userId, @Param("orgId") String orgId,
            @Param("dataPermission") DeptDataPermissionDTO dataPermission);

    FollowUpRecord selectRecord(@Param("customerId") String customerId, @Param("opportunityId") String opportunityId,
            @Param("clueId") String clueId, @Param("orgId") String orgId, @Param("type") String type);

    Long getNewContactCount(@Param("request") HomeStatisticSearchWrapperRequest request);

    /**
     * 批量合并客户(商机)记录
     *
     * @param request 请求参数
     * @param userId  用户ID
     * @param orgId   组织ID
     */
    void batchMerge(@Param("request") CustomerMergeRequest request, @Param("userId") String userId,
            @Param("orgId") String orgId);

    /**
     * 获取待合并的记录列表
     *
     * @param request 请求参数
     * @param orgId   组织ID
     * @return 记录列表
     */
    List<FollowUpRecord> getMergeRecordList(@Param("request") CustomerMergeRequest request,
            @Param("orgId") String orgId);

}
