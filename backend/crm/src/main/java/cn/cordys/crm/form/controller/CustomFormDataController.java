package cn.cordys.crm.form.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.form.dto.request.CustomFormDataAddRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataBatchUpdateRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataPageRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataUpdateRequest;
import cn.cordys.crm.form.dto.response.CustomFormDataGetResponse;
import cn.cordys.crm.form.dto.response.CustomFormDataListResponse;
import cn.cordys.crm.form.domain.CustomFormData;
import cn.cordys.crm.form.service.CustomFormDataService;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "自定义表单数据")
@RestController
@RequestMapping("/custom-form/data")
public class CustomFormDataController {

    @Resource
    private CustomFormDataService customFormDataService;
    @Resource
    private BaseMapper<ModuleForm> moduleFormMapper;

    @PostMapping("/page")
    @Operation(summary = "表单数据列表")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public PagerWithOption<List<CustomFormDataListResponse>> page(@Validated @RequestBody CustomFormDataPageRequest request) {
        ConditionFilterUtils.parseCondition(request, request.getCustomFormId());
        return customFormDataService.page(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), true);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "表单数据详情")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public CustomFormDataGetResponse get(@PathVariable String id) {
        return customFormDataService.get(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @Operation(summary = "创建表单数据")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public CustomFormData add(@Validated @RequestBody CustomFormDataAddRequest request) {
        return customFormDataService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @Operation(summary = "更新表单数据")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void update(@Validated @RequestBody CustomFormDataUpdateRequest request) {
        customFormDataService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "删除表单数据")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void delete(@PathVariable String id) {
        customFormDataService.delete(id, SessionUtils.getUserId());
    }

    @PostMapping("/batch/update")
    @Operation(summary = "批量更新表单数据")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void batchUpdate(@Validated @RequestBody CustomFormDataBatchUpdateRequest request) {
        customFormDataService.batchUpdate(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch/delete")
    @Operation(summary = "批量删除表单数据")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void batchDelete(@RequestBody List<String> ids) {
        customFormDataService.batchDelete(ids, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
