package cn.cordys.crm.form.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.form.domain.CustomForm;
import cn.cordys.crm.form.dto.request.CustomFormAddRequest;
import cn.cordys.crm.form.dto.request.CustomFormAdminBatchRequest;
import cn.cordys.crm.form.dto.request.CustomFormUpdateRequest;
import cn.cordys.crm.form.dto.response.CustomFormGetResponse;
import cn.cordys.crm.form.dto.response.CustomFormListResponse;
import cn.cordys.crm.form.service.CustomFormService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "自定义表单")
@RestController
@RequestMapping("/custom-form")
public class CustomFormController {

    @Resource
    private CustomFormService customFormService;
    @Resource
    private ResourcePermissionService resourcePermissionService;

    @GetMapping("/list")
    @Operation(summary = "自定义表单列表")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public List<CustomFormListResponse> list() {
        return customFormService.list(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/option")
    @Operation(summary = "自定义表单选项(开启的)")
    public List<OptionDTO> getOptions() {
        return customFormService.getOptions(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/option/all")
    @Operation(summary = "自定义表单选项(包含未开启的)")
    @RequiresPermissions(value = { PermissionConstants.PROCESS_SETTING_READ }, logical = Logical.OR)
    public List<OptionDTO> getAllOptions() {
        return customFormService.getAllOptions(OrganizationContext.getOrganizationId());
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "自定义表单详情")
    public CustomFormGetResponse get(@PathVariable String id) {
        String userId = SessionUtils.getUserId();
        String orgId = OrganizationContext.getOrganizationId();
        if (!resourcePermissionService.hasApprovalCommentMentionFormPermission(id, userId, orgId)) {
            resourcePermissionService.checkPermission(PermissionConstants.CUSTOM_FORM_READ);
        }
        return customFormService.get(id, userId, orgId);
    }

    @PostMapping("/add")
    @Operation(summary = "创建自定义表单")
    @CsPermission(PermissionConstants.CUSTOM_FORM_ADD)
    public CustomForm create(@Validated @RequestBody CustomFormAddRequest request) {
        return customFormService.create(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @Operation(summary = "更新自定义表单")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void update(@Validated @RequestBody CustomFormUpdateRequest request) {
        customFormService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "删除自定义表单")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void delete(@PathVariable String id) {
        customFormService.delete(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/enable/{id}")
    @Operation(summary = "启用自定义表单")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void enable(@PathVariable String id) {
        customFormService.updateEnable(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), true);
    }

    @GetMapping("/disable/{id}")
    @Operation(summary = "禁用自定义表单")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void disable(@PathVariable String id) {
        customFormService.updateEnable(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), false);
    }

    @PostMapping("/admin/set")
    @Operation(summary = "设置表单管理员")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void setAdmins(@Validated @RequestBody CustomFormAdminBatchRequest request) {
        customFormService.setAdmins(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/admin/get/{customFormId}")
    @Operation(summary = "查询表单管理员")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public List<OptionDTO> getAdmins(@PathVariable String customFormId) {
        return customFormService.getAdmins(customFormId, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
    }
}
