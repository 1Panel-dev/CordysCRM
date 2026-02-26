package cn.cordys.crm.order.mapper;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.crm.order.dto.request.OrderPageRequest;
import cn.cordys.crm.order.dto.response.OrderGetResponse;
import cn.cordys.crm.order.dto.response.OrderListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtOrderMapper {


    List<OrderListResponse> list(@Param("request") OrderPageRequest request, @Param("orgId") String orgId,
                                 @Param("userId") String userId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission, @Param("source") boolean source);

    OrderGetResponse getDetail(@Param("id") String id);

    List<OrderListResponse> getListByIds(@Param("ids") List<String> ids, @Param("userId") String userId, @Param("orgId") String orgId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    void updateStatus(@Param("id") String id, @Param("status") String status, @Param("userId") String userId, @Param("updateTime") long updateTime);
}
