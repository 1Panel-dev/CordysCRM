package cn.cordys.crm.approval.mapper;

import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.request.ApprovalFlowPageRequest;
import cn.cordys.crm.approval.dto.response.ApprovalFlowListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface ExtApprovalTaskMapper {


    void updateTaskById(@Param("approvalTask") ApprovalTask approvalTask);
}