package cn.cordys.crm.contract.controller;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.contract.domain.BusinessTitle;
import cn.cordys.crm.contract.dto.request.*;
import cn.cordys.crm.contract.dto.response.BusinessTitleListResponse;
import cn.cordys.crm.contract.service.BusinessTitleExportService;
import cn.cordys.crm.contract.service.BusinessTitleService;
import cn.cordys.crm.system.constants.ExportConstants;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "工商抬头")
@RestController
@RequestMapping("/contract/business-title")
public class BusinessTitleController {

    @Resource
    private BusinessTitleService businessTitleService;
    @Resource
    private BusinessTitleExportService businessTitleExportService;


    @GetMapping("/module/form")
    @Operation(summary = "获取表单配置")
    public ModuleFormConfigDTO getModuleFormConfig() {
        return businessTitleService.getBusinessFormConfig();
    }


    @PostMapping("/add")
    @RequiresPermissions(PermissionConstants.CONTRACT_BUSINESS_TITLE_ADD)
    @Operation(summary = "创建")
    public BusinessTitle add(@Validated @RequestBody BusinessTitleAddRequest request) {
        return businessTitleService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/update")
    @RequiresPermissions(PermissionConstants.CONTRACT_BUSINESS_TITLE_UPDATE)
    @Operation(summary = "更新")
    public BusinessTitle update(@Validated @RequestBody BusinessTitleUpdateRequest request) {
        return businessTitleService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @RequiresPermissions(PermissionConstants.CONTRACT_BUSINESS_TITLE_DELETE)
    @Operation(summary = "删除")
    public void delete(@PathVariable("id") String id) {
        businessTitleService.delete(id);
    }


    @GetMapping("/invoice/check/{id}")
    @RequiresPermissions(PermissionConstants.CONTRACT_BUSINESS_TITLE_DELETE)
    @Operation(summary = "检查工商抬头是否开过票")
    public boolean checkInvoice(@PathVariable String id) {
        return businessTitleService.checkHasInvoice(id);
    }


    @PostMapping("/page")
    @RequiresPermissions(PermissionConstants.CONTRACT_BUSINESS_TITLE_READ)
    @Operation(summary = "列表")
    public Pager<List<BusinessTitleListResponse>> list(@Validated @RequestBody BusinessTitlePageRequest request) {
        ConditionFilterUtils.parseCondition(request);
        return businessTitleService.list(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/get/{id}")
    @RequiresPermissions(PermissionConstants.CONTRACT_BUSINESS_TITLE_READ)
    @Operation(summary = "详情")
    public BusinessTitleListResponse get(@PathVariable("id") String id) {
        return businessTitleService.get(id);
    }


    @PostMapping("/approval")
    @RequiresPermissions(PermissionConstants.CONTRACT_BUSINESS_TITLE_APPROVAL)
    @Operation(summary = "审核通过/不通过")
    public void approval(@Validated @RequestBody BusinessTitleApprovalRequest request) {
        businessTitleService.approvalContract(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/revoke/{id}")
    @Operation(summary = "撤销审批")
    public String revoke(@PathVariable("id") String id) {
        return businessTitleService.revoke(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/export-select")
    @Operation(summary = "导出选中工商抬头")
    @RequiresPermissions(PermissionConstants.CONTRACT_BUSINESS_TITLE_EXPORT)
    public String exportSelect(@Validated @RequestBody ExportSelectRequest request) {
        ExportDTO exportDTO = ExportDTO.builder()
                .exportType(ExportConstants.ExportType.BUSINESS_TITLE.name())
                .fileName(request.getFileName())
                .headList(request.getHeadList())
                .logModule(LogModule.CONTRACT_BUSINESS_TITLE)
                .locale(LocaleContextHolder.getLocale())
                .orgId(OrganizationContext.getOrganizationId())
                .userId(SessionUtils.getUserId())
                .selectIds(request.getIds())
                .selectRequest(request)
                .build();
        return businessTitleExportService.exportSelect(exportDTO);
    }


    @PostMapping("/export-all")
    @Operation(summary = "导出全部")
    @RequiresPermissions(PermissionConstants.CONTRACT_BUSINESS_TITLE_EXPORT)
    public String exportAll(@Validated @RequestBody BusinessTitleExportRequest request) {
        ConditionFilterUtils.parseCondition(request);
        ExportDTO exportDTO = ExportDTO.builder()
                .exportType(ExportConstants.ExportType.BUSINESS_TITLE.name())
                .fileName(request.getFileName())
                .headList(request.getHeadList())
                .logModule(LogModule.CONTRACT_BUSINESS_TITLE)
                .locale(LocaleContextHolder.getLocale())
                .orgId(OrganizationContext.getOrganizationId())
                .userId(SessionUtils.getUserId())
                .pageRequest(request)
                .build();
        return businessTitleExportService.export(exportDTO);
    }


    @GetMapping("/template/download")
    @RequiresPermissions(PermissionConstants.CONTRACT_BUSINESS_TITLE_IMPORT)
    @Operation(summary = "下载导入模板")
    public void downloadImportTpl(HttpServletResponse response) {
        businessTitleService.downloadImportTpl(response, OrganizationContext.getOrganizationId());
    }


    @PostMapping("/import/pre-check")
    @Operation(summary = "excel导入检查")
    @RequiresPermissions(PermissionConstants.CONTRACT_BUSINESS_TITLE_IMPORT)
    public ImportResponse preCheck(@RequestPart(value = "file", required = false) MultipartFile file) {
        return businessTitleService.importPreCheck(file, OrganizationContext.getOrganizationId());
    }


    @PostMapping(value = "/import")
    @Operation(summary = "导入")
    @RequiresPermissions(PermissionConstants.CONTRACT_BUSINESS_TITLE_IMPORT)
    public ImportResponse realImport(@RequestPart(value = "file", required = false) MultipartFile file) {
        return businessTitleService.realImport(file, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @PostMapping("/third-query/option")
    @RequiresPermissions(PermissionConstants.CONTRACT_BUSINESS_TITLE_ADD)
    @Operation(summary = "第三方接口分页模糊查询工商名称")
    public Pager<List<String>> thirdQueryOptions(@Validated @RequestBody BasePageRequest request) {
        return businessTitleService.thirdQueryOption(request.getKeyword(), request.getCurrent(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/third-query")
    @RequiresPermissions(PermissionConstants.CONTRACT_BUSINESS_TITLE_ADD)
    @Operation(summary = "第三方接口查询工商抬头信息")
    public BusinessTitle thirdQuery(@RequestParam String keyword) {
        return businessTitleService.thirdQuery(keyword, OrganizationContext.getOrganizationId());
    }
}
