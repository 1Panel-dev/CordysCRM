package cn.cordys.crm.form.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.form.domain.CustomFormData;
import cn.cordys.crm.form.dto.request.*;
import cn.cordys.crm.form.dto.response.CustomFormDataGetResponse;
import cn.cordys.crm.form.dto.response.CustomFormDataListResponse;
import cn.cordys.crm.form.service.CustomFormDataExportService;
import cn.cordys.crm.form.service.CustomFormDataService;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "自定义表单数据")
@RestController
@RequestMapping("/custom-form/data")
public class CustomFormDataController {

    @Resource
    private CustomFormDataService customFormDataService;
    @Resource
    private CustomFormDataExportService customFormDataExportService;

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

    @PostMapping("/export-all")
    @Operation(summary = "导出全部表单数据")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public String exportAll(@Validated @RequestBody CustomFormDataExportRequest request) {
        ConditionFilterUtils.parseCondition(request, request.getCustomFormId());
        return customFormDataExportService.exportAll(request, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), LocaleContextHolder.getLocale());
    }

    @PostMapping("/export-select")
    @Operation(summary = "导出选中表单数据")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public String exportSelect(@Validated @RequestBody ExportSelectRequest request) {
        return customFormDataExportService.exportSelect(request, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), LocaleContextHolder.getLocale());
    }

    @GetMapping("/template/download")
    @Operation(summary = "下载导入模板")
    public void downloadImportTpl(@RequestParam String customFormId, HttpServletResponse response) {
        customFormDataService.downloadImportTpl(response, customFormId, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/import/pre-check")
    @Operation(summary = "导入预检查")
    public ImportResponse importPreCheck(@RequestParam String customFormId,
                                         @RequestPart(value = "file") MultipartFile file) {
        return customFormDataService.importPreCheck(file, customFormId, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/import")
    @Operation(summary = "导入表单数据")
    public ImportResponse realImport(@RequestParam String customFormId,
                                     @RequestPart(value = "file") MultipartFile file) {
        return customFormDataService.realImport(file, customFormId,
                OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }
}
