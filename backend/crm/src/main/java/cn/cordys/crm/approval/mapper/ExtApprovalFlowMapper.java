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
     * 校验同一表单是否存在启用的审批流
     */
    boolean checkEnableFlowExists(
            @Param("formType") String formType,
            @Param("organizationId") String organizationId);

    /**
     * 校验同一表单是否存在启用的审批流（排除指定ID）
     */
    boolean checkEnableFlowExistsExcludeId(
            @Param("formType") String formType,
            @Param("organizationId") String organizationId,
            @Param("excludeId") String excludeId);
}