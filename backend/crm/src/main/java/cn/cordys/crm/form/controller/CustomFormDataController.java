package cn.cordys.crm.form.controller;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.common.constants.InternalUserView;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.condition.BaseCondition;
import cn.cordys.common.dto.condition.CombineSearch;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.form.domain.CustomFormData;
import cn.cordys.crm.form.dto.request.CustomFormDataAddRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataBatchUpdateRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataExportRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataPageRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataUpdateRequest;
import cn.cordys.crm.form.dto.request.CustomFormExportSelectRequest;
import cn.cordys.crm.form.dto.request.CustomerFormImportRequest;
import cn.cordys.crm.form.dto.response.CustomFormDataGetResponse;
import cn.cordys.crm.form.dto.response.CustomFormDataListResponse;
import cn.cordys.crm.form.service.CustomFormDataExportService;
import cn.cordys.crm.form.service.CustomFormDataService;
import cn.cordys.crm.form.service.CustomFormViewScopeService;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.dto.response.BatchAffectReasonResponse;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.service.UserViewService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
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
    @Resource
    private CustomFormViewScopeService customFormViewScopeService;
    @Resource
    private UserViewService userViewService;

    @PostMapping("/page")
    @Operation(summary = "表单数据列表")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public PagerWithOption<List<CustomFormDataListResponse>> page(
            @Validated @RequestBody CustomFormDataPageRequest request) {
        String userId = SessionUtils.getUserId();
        String orgId = OrganizationContext.getOrganizationId();
        prepareViewCondition(request, request.getCustomFormId(), userId, orgId);
        ConditionFilterUtils.parseCondition(request, request.getCustomFormId());
        return customFormDataService.page(request, userId, orgId, false);
    }

    /**
     * 处理自定义表单视图: 校验自定义视图归属当前表单, 并注入内置系统视图(全部/部门/我的)的查询条件。
     * 注入的条件由后续 {@link ConditionFilterUtils#parseCondition} 统一转换。
     */
    private void prepareViewCondition(BaseCondition request, String customFormId, String userId, String orgId) {
        String viewId = request.getViewId();
        if (StringUtils.isNotBlank(viewId) && !InternalUserView.isInternalUserView(viewId)) {
            userViewService.checkView(userId, viewId, orgId, customFormId);
            return;
        }
        // 单个部门/成员条件已在此处算好部门及子部门, 无需再走 containChildIds 展开
        CombineSearch internalView = customFormViewScopeService.resolveInternalView(viewId, userId, orgId);
        if (internalView != null) {
            request.setViewCombineSearch(internalView);
        }
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "表单数据详情")
    public CustomFormDataGetResponse get(@PathVariable String id) {
        return customFormDataService.get(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/create-permission/{customFormId}")
    @Operation(summary = "查询当前用户是否有创建表单数据的权限")
    public boolean hasCreatePermission(@PathVariable String customFormId) {
        return customFormDataService.hasCreatePermission(customFormId, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
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
    public CustomFormData update(@Validated @RequestBody CustomFormDataUpdateRequest request) {
        return customFormDataService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId(),
                true);
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "删除表单数据")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void delete(@PathVariable String id) {
        customFormDataService.deleteWithApprovalCheck(id, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch/update")
    @Operation(summary = "批量更新表单数据")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public BatchAffectReasonResponse batchUpdate(@Validated @RequestBody CustomFormDataBatchUpdateRequest request) {
        return customFormDataService.batchUpdate(request, SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId());
    }

    @PostMapping("/batch/delete")
    @Operation(summary = "批量删除表单数据")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public void batchDelete(@RequestBody @NotEmpty List<String> ids) {
        customFormDataService.batchDelete(ids, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/export-all")
    @Operation(summary = "导出全部表单数据")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public String exportAll(@Validated @RequestBody CustomFormDataExportRequest request) {
        String userId = SessionUtils.getUserId();
        String orgId = OrganizationContext.getOrganizationId();
        prepareViewCondition(request, request.getCustomFormId(), userId, orgId);
        ConditionFilterUtils.parseCondition(request, request.getCustomFormId());
        ExportDTO exportDTO = ExportDTO.builder()
                .exportType(ExportConstants.ExportType.CUSTOM_FORM_DATA.name())
                .fileName(request.getFileName())
                .headList(request.getHeadList())
                .logModule(LogModule.CUSTOM_FORM_DATA)
                .locale(LocaleContextHolder.getLocale())
                .orgId(orgId)
                .userId(userId)
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
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public ImportResponse importPreCheck(@Validated @RequestPart("request") CustomerFormImportRequest request,
            @RequestPart(value = "file") MultipartFile file) {
        return customFormDataService.importPreCheck(file, request, OrganizationContext.getOrganizationId(),
                SessionUtils.getUserId());
    }

    @PostMapping("/import")
    @Operation(summary = "导入表单数据")
    @CsPermission(PermissionConstants.CUSTOM_FORM_READ)
    public ImportResponse realImport(@Validated @RequestPart("request") CustomerFormImportRequest request,
            @RequestPart(value = "file") MultipartFile file) {
        return customFormDataService.realImport(file, request,
                OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }
}
