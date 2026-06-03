package cn.cordys.crm.approval.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.dto.WebHookConfig;
import cn.cordys.crm.approval.dto.request.ApprovalFlowAddRequest;
import cn.cordys.crm.approval.dto.request.ApprovalFlowPageRequest;
import cn.cordys.crm.approval.dto.request.ApprovalFlowUpdateRequest;
import cn.cordys.crm.approval.dto.response.ApprovalFlowByFormTypeResponse;
import cn.cordys.crm.approval.dto.response.ApprovalFlowDetailResponse;
import cn.cordys.crm.approval.dto.response.ApprovalFlowListResponse;
import cn.cordys.crm.approval.dto.response.StatusPermissionSettingResponse;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *  @author jianxing ai
 */
@Tag(name = "审批流设置")
@RestController
@RequestMapping("/approval-flow")
public class ApprovalFlowController {

    @Resource
    private ApprovalFlowService approvalFlowService;

    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.PROCESS_SETTING_READ)
    @Operation(summary = "审批流列表")
    public Pager<List<ApprovalFlowListResponse>> list(@Validated @RequestBody ApprovalFlowPageRequest request) {
        return approvalFlowService.list(request, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.PROCESS_SETTING_ADD)
    @Operation(summary = "新建审批流")
    public ApprovalFlowDetailResponse add(@Validated @RequestBody ApprovalFlowAddRequest request) {
        return approvalFlowService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.PROCESS_SETTING_UPDATE)
    @Operation(summary = "更新审批流")
    public ApprovalFlowDetailResponse update(@Validated @RequestBody ApprovalFlowUpdateRequest request) {
        return approvalFlowService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.PROCESS_SETTING_DELETE)
    @Operation(summary = "删除审批流")
    public void delete(@PathVariable("id") String id) {
        approvalFlowService.delete(id, OrganizationContext.getOrganizationId());
    }

    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.PROCESS_SETTING_READ)
    @Operation(summary = "获取审批流详情")
    public ApprovalFlowDetailResponse get(@PathVariable("id") String id) {
        return approvalFlowService.getDetail(id, OrganizationContext.getOrganizationId());
    }

    @GetMapping("/enable/{id}")
    @RequiresPermissions(PermissionConstants.PROCESS_SETTING_UPDATE)
    @Operation(summary = "启用/禁用审批流")
    public void updateEnable(@PathVariable("id") String id, @RequestParam("enable") Boolean enable) {
        approvalFlowService.updateEnable(id, enable, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/status-permission/setting/{formType}")
    @Operation(summary = "根据表单类型获取审批流状态权限配置")
    public StatusPermissionSettingResponse getStatusPermissionSetting(@PathVariable("formType") String formType) {
        return approvalFlowService.getStatusPermissionsByFormType(formType, OrganizationContext.getOrganizationId());
    }

    @GetMapping("/get-by-form-type/{formType}")
    @Operation(summary = "根据表单类型获取审批流信息")
    public ApprovalFlowByFormTypeResponse getByFormType(@PathVariable("formType") String formType) {
        return approvalFlowService.getByFormType(formType, OrganizationContext.getOrganizationId());
    }


    @PostMapping("/webhook/test")
    @Operation(summary = "webhook-测试连接")
    public void verifyEmailConnection(@Validated @RequestBody WebHookConfig webHookConfig) {
        approvalFlowService.testConnection(webHookConfig);
    }
}