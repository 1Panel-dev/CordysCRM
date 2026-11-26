package cn.cordys.crm.order.mapper;

import cn.cordys.common.dto.BatchUpdateDbParam;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.condition.BaseCondition;
import cn.cordys.common.statistic.StatisticSqlMapper;
import cn.cordys.crm.customer.dto.request.CustomerMergeRequest;
import cn.cordys.crm.order.domain.Order;
import cn.cordys.crm.order.dto.request.OrderPageRequest;
import cn.cordys.crm.order.dto.response.OrderGetResponse;
import cn.cordys.crm.order.dto.response.OrderListResponse;
import cn.cordys.crm.order.dto.response.OrderStatisticResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtOrderMapper extends StatisticSqlMapper {

    List<OrderListResponse> list(@Param("request") OrderPageRequest request, @Param("orgId") String orgId,
            @Param("userId") String userId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission,
            @Param("source") boolean source);

    OrderGetResponse getDetail(@Param("id") String id);

    List<OrderListResponse> getListByIds(@Param("ids") List<String> ids, @Param("userId") String userId,
            @Param("orgId") String orgId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    void batchUpdate(@Param("request") BatchUpdateDbParam request);

    int countByStage(@Param("stage") String stage);

    OrderStatisticResponse searchStatistic(@Param("request") BaseCondition request, @Param("orgId") String orgId,
            @Param("userId") String userId, @Param("dataPermission") DeptDataPermissionDTO dataPermission);

    void moveUpStageOrder(@Param("end") Long end, @Param("stage") String stage, @Param("pos") Long pos);

    void moveDownStageOrder(@Param("end") Long end, @Param("stage") String stage, @Param("pos") Long pos);

    Long selectNextPos(@Param("orgId") String orgId, @Param("stage") String stage);

    void updateOldApprovalStatusNone();

    void updateOrder(@Param("order") Order order);

    /**
     * 审批驳回/撤回时回退阶段
     *
     * @param id         订单ID
     * @param stage      回退到的阶段
     * @param userId     用户ID
     * @param updateTime 更新时间
     */
    void revertStageByApproval(@Param("id") String id, @Param("stage") String stage, @Param("userId") String userId,
            @Param("updateTime") long updateTime);

    /**
     * 批量合并客户订单
     *
     * @param request 请求参数
     * @param userId  用户ID
     * @param orgId   组织ID
     */
    void batchMerge(@Param("request") CustomerMergeRequest request, @Param("userId") String userId,
            @Param("orgId") String orgId);

    /**
     * 获取待合并的客户订单列表
     *
     * @param request 请求参数
     * @param orgId   组织ID
     * @return 客户订单列表
     */
    List<Order> getMergeOrderList(@Param("request") CustomerMergeRequest request, @Param("orgId") String orgId);
}
