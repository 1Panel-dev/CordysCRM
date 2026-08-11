package cn.cordys.crm.form.controller;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.ExportDTO;
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
import cn.cordys.crm.system.constants.ExportConstants;
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
        return customFormDataService.page(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(), false);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "表单数据详情")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public CustomFormDataGetResponse get(@PathVariable String id) {
        return customFormDataService.get(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/create-permission/{customFormId}")
    @Operation(summary = "查询当前用户是否有创建表单数据的权限")
    public boolean hasCreatePermission(@PathVariable String customFormId) {
        return customFormDataService.hasCreatePermission(customFormId, SessionUtils.getUserId());
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
        ExportDTO exportDTO = ExportDTO.builder()
                .exportType(ExportConstants.ExportType.CUSTOM_FORM_DATA.name())
                .fileName(request.getFileName())
                .headList(request.getHeadList())
                .logModule(LogModule.CUSTOM_FORM_DATA)
                .locale(LocaleContextHolder.getLocale())
                .orgId(OrganizationContext.getOrganizationId())
                .userId(SessionUtils.getUserId())
                .pageRequest(request)
                .formKey(request.getCustomFormId())
                .build();
        return customFormDataExportService.exportAllWithMergeStrategy(exportDTO);
    }

    @PostMapping("/export-select")
    @Operation(summary = "导出选中表单数据")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public String exportSelect(@Validated @RequestBody CustomFormExportSelectRequest request) {
        ExportDTO exportDTO = ExportDTO.builder()
                .exportType(ExportConstants.ExportType.CUSTOM_FORM_DATA.name())
                .fileName(request.getFileName())
                .headList(request.getHeadList())
                .logModule(LogModule.CUSTOM_FORM_DATA)
                .locale(LocaleContextHolder.getLocale())
                .orgId(OrganizationContext.getOrganizationId())
                .userId(SessionUtils.getUserId())
                .selectIds(request.getIds())
                .selectRequest(request)
                .formKey(request.getCustomFormId())
                .build();
        return customFormDataExportService.exportSelectWithMergeStrategy(exportDTO);
    }

    @GetMapping("/template/download")
    @Operation(summary = "下载导入模板")
    public void downloadImportTpl(@RequestParam String customFormId, HttpServletResponse response) {
        customFormDataService.downloadImportTpl(response, customFormId, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/import/pre-check")
    @Operation(summary = "导入预检查")
    public ImportResponse importPreCheck(@Validated @RequestPart("request") CustomerFormImportRequest request,
                                         @RequestPart(value = "file") MultipartFile file) {
        return customFormDataService.importPreCheck(file, request, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/import")
    @Operation(summary = "导入表单数据")
    public ImportResponse realImport(@Validated @RequestPart("request") CustomerFormImportRequest request,
                                     @RequestPart(value = "file") MultipartFile file) {
        return customFormDataService.realImport(file, request,
                OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }
}
