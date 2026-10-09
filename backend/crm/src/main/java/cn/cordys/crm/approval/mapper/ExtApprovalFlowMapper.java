package cn.cordys.crm.approval.mapper;

import cn.cordys.crm.approval.dto.request.ApprovalFlowPageRequest;
import cn.cordys.crm.approval.dto.response.ApprovalFlowListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审批流扩展Mapper
 */
public interface ExtApprovalFlowMapper {

    /**
     * 分页查询审批流列表（带用户名称）
     */
    List<ApprovalFlowListResponse> list(
            @Param("request") ApprovalFlowPageRequest request,
            @Param("organizationId") String organizationId);

    /**
     * 获取同组织、同前缀的最大编号序号，包含已删除的审批流，避免重复使用历史编号。
     */
    long getMaxNumberSequence(@Param("organizationId") String organizationId, @Param("prefix") String prefix);
}
